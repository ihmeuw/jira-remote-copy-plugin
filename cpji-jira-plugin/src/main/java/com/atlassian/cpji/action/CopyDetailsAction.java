package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.EntityLink;
import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.rest.UnauthorizedResponseException;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.RemoteUserBean;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @since v1.4
 */
public class CopyDetailsAction extends AbstractCopyIssueAction
{
    private Boolean remoteAttachmentsEnabled;
    private Boolean hasCreateAttachmentsPermission;

    private String remoteUserName;
    private String remoteFullUserName;

    private Collection<Option> availableIssueTypes;
    private List<Option> issueLinkOptions;
    private final I18nHelper.BeanFactory beanFactory;

    public class Option
    {
        private final String value;
        private final boolean selected;
        private String label;

        Option(String value, boolean selected)
        {
            this.value = value;
            this.selected = selected;
        }

        Option(String value, boolean selected, final String label)
        {
            this.value = value;
            this.selected = selected;
            this.label = label;
        }

        public String getValue()
        {
            return value;
        }

        public boolean isSelected()
        {
            return selected;
        }

        public String getLabel()
        {
            return label;
        }
    }


    public CopyDetailsAction(
            final SubTaskManager subTaskManager,
            final EntityLinkService entityLinkService,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final BeanFactory beanFactory)
    {
        super(subTaskManager, entityLinkService, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory, fieldLayoutItemsRetriever, copyIssuePermissionManager);
        this.beanFactory = beanFactory;
    }

    @Override
    protected String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        EntityLink entityLink = getSelectedEntityLink();
        if (entityLink == null)
        {
            addErrorMessage("Failed to find the entity link.");
            return ERROR;
        }
        ApplicationLinkRequestFactory requestFactory = entityLink.getApplicationLink().createAuthenticatedRequestFactory();
        try
        {
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, REST_URL_COPY_ISSUE + COPY_ISSUE_RESOURCE_PATH + "/issueTypeInformation/" + entityLink.getKey());
            CopyInformationBean copyInformationBean = request.execute(new ApplicationLinkResponseHandler<CopyInformationBean>()
            {
                public CopyInformationBean credentialsRequired(final Response response) throws ResponseException
                {
                    return response.getEntity(CopyInformationBean.class);
                }

                public CopyInformationBean handle(final Response response) throws ResponseException
                {
                    if (!response.isSuccessful() && (response.getStatusCode() == 401))
                    {
                        throw new UnauthorizedResponseException();
                    }
                    else if (!response.isSuccessful())
                    {
                       throw new ResponseException(response.getResponseBodyAsString());
                    }
                    return response.getEntity(CopyInformationBean.class);
                }
            });
            if (!copyInformationBean.getHasCreateIssuePermission())
            {
                addErrorMessage("You don't have the create issue permission for this JIRA project!");
                return ERROR;
            }
            issueLinkOptions = new ArrayList<Option>();
            I18nHelper i18nHelper = beanFactory.getInstance(getLoggedInUser());
            String remoteJiraVersion = copyInformationBean.getVersion();
            if (remoteJiraVersion != null && remoteJiraVersion.startsWith("5.0"))
            {
                issueLinkOptions.add(new Option(RemoteIssueLinkType.RECIPROCAL.name(), false, i18nHelper.getText(RemoteIssueLinkType.RECIPROCAL.getI18nKey())));
                issueLinkOptions.add(new Option(RemoteIssueLinkType.INCOMING.name(), false, i18nHelper.getText(RemoteIssueLinkType.INCOMING.getI18nKey())));
            }
            issueLinkOptions.add(new Option(RemoteIssueLinkType.OUTGOING.name(), false, i18nHelper.getText(RemoteIssueLinkType.OUTGOING.getI18nKey())));
            issueLinkOptions.add(new Option(RemoteIssueLinkType.NONE.name(), false, i18nHelper.getText(RemoteIssueLinkType.NONE.getI18nKey())));
            checkIssueTypes(copyInformationBean.getIssueTypes().getGetTypes());
            remoteAttachmentsEnabled = copyInformationBean.getAttachmentsEnabled();
            hasCreateAttachmentsPermission = copyInformationBean.getHasCreateAttachmentPermission();
            RemoteUserBean user = copyInformationBean.getRemoteUser();
            remoteUserName = user.getUserName();
            remoteFullUserName = copyInformationBean.getRemoteUser().getFullName();
        }
        catch (CredentialsRequiredException e)
        {
            log.error("OAuth token invalid.", e);
            addErrorMessage("OAuth token invalid. Reason:" + e.getMessage());
            return ERROR;
        }
        catch (UnauthorizedResponseException e)
        {
            log.error("Authentication failed.", e);
            addErrorMessage("Authentication failed. If using Trusted Apps, do you have a user with the same user name in the remote JIRA instance?");
            return ERROR;
        }
        catch (ResponseException e)
        {
            log.error("Failed to retrieve the list of issue fields from the remote JIRA instance.", e);
            addErrorMessage("Failed to retrieve the list of issue fields from the remote JIRA instance.");
            addErrorMessage(e.getMessage());
            return ERROR;
        }
        return SUCCESS;
    }

    public String getRemoteUserName()
    {
        return remoteUserName;
    }

    public String getRemoteFullUserName()
    {
        return remoteFullUserName;
    }

    public boolean attachmentsEnabled()
    {
        return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
    }

    public boolean remoteAttachmentsEnabled()
    {
        return remoteAttachmentsEnabled;
    }

    public Boolean hasCreateAttachmentsPermission()
    {
        return hasCreateAttachmentsPermission;
    }

    public boolean isSALUpgradeRequired()
    {
        return false;
    }

    private void checkIssueTypes(final Collection<String> values)
    {
        MutableIssue issue = getIssueObject();
        availableIssueTypes = new ArrayList<Option>();
        for (String value : values)
        {
            if (value.equals(issue.getIssueTypeObject().getName()))
            {
                availableIssueTypes.add(new Option(value, true));
            }
            else
            {
                availableIssueTypes.add(new Option(value, false));
            }
        }
    }

    public List<Option> getIssueLinkOptions()
    {
        return issueLinkOptions;
    }

    public Collection<Option> getAvailableIssueTypes()
    {
        return availableIssueTypes;
    }
}
