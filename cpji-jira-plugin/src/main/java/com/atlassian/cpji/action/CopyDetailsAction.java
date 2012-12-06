package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.ResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @since v1.4
 */
public class CopyDetailsAction extends AbstractCopyIssueAction
{
    private String remoteUserName;
    private String remoteFullUserName;

    private Collection<Option> availableIssueTypes;
	private final IssueLinkManager issueLinkManager;

	private CopyInformationBean copyInfo;

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
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager,
			final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory,
			final WebResourceManager webResourceManager,
			final IssueLinkManager issueLinkManager
    )
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory, fieldLayoutItemsRetriever,
				copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory, webResourceManager);
		this.issueLinkManager = issueLinkManager;
	}

	public boolean isIssueWithComments() {
		final MutableIssue issue = getIssueObject();
		if (issue != null) {
			return !commentManager.getComments(issue).isEmpty();
		}
		return false;
	}

	public boolean isIssueWithAttachments() {
		final MutableIssue issue = getIssueObject();
		if (issue != null) {
			return !issue.getAttachments().isEmpty();
		}
		return false;
	}

	public boolean isIssueWithLinks() {
		final MutableIssue issue = getIssueObject();
		if (issueLinkManager.isLinkingEnabled()) {
			if (issue != null) {
				return !issueLinkManager.getOutwardLinks(issue.getId()).isEmpty();
			}
		}
		return false;
	}

    @Override
    protected String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        SelectedProject entityLink = getSelectedDestinationProject();
        if (entityLink == null)
        {
            addErrorMessage("Failed to find the entity link.");
            return ERROR;
        }

        JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());
        Either<ResponseStatus, CopyInformationBean> result = proxy.getCopyInformation(entityLink.getProjectKey());
        if(result.isLeft()) {
            ResponseStatus status = (ResponseStatus) result.left().get();
            if(ResponseStatus.Status.AUTHENTICATION_FAILED.equals(status.getResult())){
                log.error("Authentication failed.");
                addErrorMessage("Authentication failed. If using Trusted Apps, do you have a user with the same user name in the remote JIRA instance?");
            } else if(ResponseStatus.Status.AUTHORIZATION_REQUIRED.equals(status.getResult())){
                log.error("OAuth token invalid.");
            } else if(ResponseStatus.Status.COMMUNICATION_FAILED.equals(status.getResult())){
                log.error("Failed to retrieve the list of issue fields from the remote JIRA instance.");
                addErrorMessage("Failed to retrieve the list of issue fields from the remote JIRA instance.");
            }

            return ERROR;
        }

        copyInfo = (CopyInformationBean) result.right().get();
        if (!copyInfo.getHasCreateIssuePermission())
        {
            addErrorMessage(getText("cpji.you.dont.have.create.issue.permission"));
            return ERROR;
        }

        checkIssueTypes(copyInfo.getIssueTypes().getGetTypes());

		UserBean user = copyInfo.getRemoteUser();
        remoteUserName = user.getUserName();
        remoteFullUserName = copyInfo.getRemoteUser().getFullName();

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

	public boolean linksEnabled()
	{
		return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ISSUELINKING);
	}

	public CopyInformationBean getCopyInfo() {
		return copyInfo;
	}

	public boolean remoteAttachmentsEnabled()
    {
        return getCopyInfo().getAttachmentsEnabled();
    }

    public Boolean hasCreateAttachmentsPermission()
    {
        return getCopyInfo().getHasCreateAttachmentPermission();
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
		List<Option> issueLinkOptions = Lists.newArrayList();

		if (linksEnabled() && copyInfo.getIssueLinkingEnabled()) {
			issueLinkOptions.add(new Option(RemoteIssueLinkType.RECIPROCAL.name(), false, getText(RemoteIssueLinkType.RECIPROCAL.getI18nKey())));
		}
		if (copyInfo.getIssueLinkingEnabled()) {
			issueLinkOptions.add(new Option(RemoteIssueLinkType.INCOMING.name(), false, getText(RemoteIssueLinkType.INCOMING.getI18nKey())));
		}
		if (linksEnabled()) {
			issueLinkOptions.add(new Option(RemoteIssueLinkType.OUTGOING.name(), false, getText(RemoteIssueLinkType.OUTGOING.getI18nKey())));
		}
		issueLinkOptions.add(new Option(RemoteIssueLinkType.NONE.name(), false, getText(RemoteIssueLinkType.NONE.getI18nKey())));

        return issueLinkOptions;
    }

    public Collection<Option> getAvailableIssueTypes()
    {
        return availableIssueTypes;
    }

	public boolean isAdvancedSectionVisible() {
		return isCopyAttachmentsSectionVisible() || isCopyIssueLinksSectionVisible()
				|| isCreateIssueLinkSectionVisible() || isCopyCommentsSectionVisible();
	}

	public boolean isCopyAttachmentsSectionVisible() {
		return attachmentsEnabled() && isIssueWithAttachments();
	}

	public boolean isCopyIssueLinksSectionVisible() {
		return linksEnabled() && isIssueWithLinks();
	}

	public boolean isCreateIssueLinkSectionVisible() {
		return linksEnabled() || copyInfo.getIssueLinkingEnabled();
	}

	public boolean isCopyCommentsSectionVisible() {
		return isIssueWithComments();
	}

	public String getCopyAttachmentsErrorMessage() {
		if(isSALUpgradeRequired()) {
			return getText("cpji.attachments.not.moved.sal");
		} else if (!copyInfo.getAttachmentsEnabled()) {
			return getText("cpji.attachments.are.disabled");
		} else if(!copyInfo.getHasCreateAttachmentPermission()) {
			return getText("cpji.not.permitted.to.create.attachments");
		}
		return "";
	}

	public String getCopyIssueLinksErrorMessage() {
		if (!copyInfo.getAttachmentsEnabled()) {
			return getText("cpji.remote.issue.linking.is.disabled");
		}
		return "";
	}
}
