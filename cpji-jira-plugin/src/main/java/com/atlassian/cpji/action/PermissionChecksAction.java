package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @since v1.4
 */
public class PermissionChecksAction extends AbstractCopyIssueAction
{
    private String issueType;
    private List<FieldPermission> systemFieldPermissions;
    private List<FieldPermission> customFieldPermissions;
    private boolean canCopyIssue = true;
    private final FieldMapperFactory fieldMapperFactory;
    private boolean copyAttachments;
    private boolean copyIssueLinks;
    private String remoteIssueLink;

    public PermissionChecksAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldManager fieldManager,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager,
			final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory)
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory,
				fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory);
        this.fieldMapperFactory = fieldMapperFactory;
    }

    public class FieldPermission
    {
        private final String fieldName;
        private final String validationMessage;
        private final boolean canCopyIssue;

        public FieldPermission(String name, String validationMessage, final boolean canCopyIssue)
        {
            this.fieldName = name;
            this.validationMessage =  validationMessage;
            this.canCopyIssue = canCopyIssue;
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public String getValidationMessage()
        {
            return validationMessage;
        }

        public boolean canCopyIssue()
        {
            return canCopyIssue;
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        final SelectedProject entityLink = getSelectedDestinationProject();
        if (entityLink == null)
        {
            addErrorMessage("Failed to find the destination project.");
            return ERROR;
        }
        final SelectedProject linkToTargetEntity = getSelectedDestinationProject();
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(linkToTargetEntity.getJiraLocation());


        CopyIssueBean copyIssueBean = createCopyIssueBean(entityLink.getProjectKey(), getIssueObject(), issueType);
        Either<ResponseStatus, FieldPermissionsBean> result = proxy.checkPermissions(copyIssueBean);

        if(result.isLeft()){
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

        FieldPermissionsBean fieldValidationBean = (FieldPermissionsBean) result.right().get();

        List<SystemFieldPermissionBean> fieldPermissionBeans = fieldValidationBean.getSystemFieldPermissionBeans();
        systemFieldPermissions = new ArrayList<FieldPermission>();
        canCopyIssue = true;
        for (SystemFieldPermissionBean fieldPermissionBean : fieldPermissionBeans)
        {
            ValidationCode validationCode = ValidationCode.valueOf(fieldPermissionBean.getValidationCode());
            if (!validationCode.canCopyIssue() && canCopyIssue)
            {
                canCopyIssue = false;
            }

            Map<String,FieldMapper> fieldMappers = fieldMapperFactory.getSystemFieldMappers();

            FieldMapper fieldMapper = fieldMappers.get(fieldPermissionBean.getFieldId());
            if (fieldMapper != null && !ValidationCode.OK.equals(validationCode))
            {
                systemFieldPermissions.add(new FieldPermission(getI18nHelper().getText(fieldMapper.getFieldNameKey()), getI18nHelper().getText(validationCode.getI18nKey()), validationCode.canCopyIssue()));
            }
            else if (fieldMapper == null)
            {
                log.error("No support for field with id'"+ fieldPermissionBean.getFieldId() + "'");
            }
        }
        List<CustomFieldPermissionBean> customFieldPermissionBeans = fieldValidationBean.getCustomFieldPermissionBeans();
        customFieldPermissions = new ArrayList<FieldPermission>();
        if (customFieldPermissionBeans != null)
        {
            for (CustomFieldPermissionBean customFieldPermissionBean : customFieldPermissionBeans)
            {
                ValidationCode validationCode = ValidationCode.valueOf(customFieldPermissionBean.getValidationCode());
                if (!validationCode.canCopyIssue() && canCopyIssue)
                {
                    canCopyIssue = false;
                }

                if (!ValidationCode.OK.equals(validationCode))
                {
                    customFieldPermissions.add(new FieldPermission(customFieldPermissionBean.getFieldName(), getI18nHelper().getText(validationCode.getI18nKey()), validationCode.canCopyIssue()));
                }
            }
        }
        return SUCCESS;

    }

    public boolean canCopyIssue()
    {
       return canCopyIssue;
    }

    public String getIssueType()
    {
        return issueType;
    }

    public void setIssueType(final String issueType)
    {
        this.issueType = issueType;
    }

    public List<FieldPermission> getSystemFieldPermissions()
    {
        return systemFieldPermissions;
    }

    public List<FieldPermission> getCustomFieldPermissions()
    {
        return customFieldPermissions;
    }

    public boolean copyAttachments()
    {
        return copyAttachments;
    }

    public void setCopyAttachments(final boolean copyAttachments)
    {
        this.copyAttachments = copyAttachments;
    }

    public boolean copyIssueLinks()
    {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(final boolean copyIssueLinks)
    {
        this.copyIssueLinks = copyIssueLinks;
    }

    public String remoteIssueLink()
    {
        return remoteIssueLink;
    }

    public void setRemoteIssueLink(final String remoteIssueLink)
    {
        this.remoteIssueLink = remoteIssueLink;
    }
}
