package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;

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
	private final CopyIssueBeanFactory copyIssueBeanFactory;
	private boolean copyAttachments;
    private boolean copyIssueLinks;
	private boolean copyComments;
    private String remoteIssueLink;

    public PermissionChecksAction(
			final SubTaskManager subTaskManager,
			final FieldLayoutManager fieldLayoutManager,
			final CommentManager commentManager,
			final FieldMapperFactory fieldMapperFactory,
			final CopyIssuePermissionManager copyIssuePermissionManager,
			final ApplicationLinkService applicationLinkService,
			final JiraProxyFactory jiraProxyFactory,
			final WebResourceManager webResourceManager,
			final CopyIssueBeanFactory copyIssueBeanFactory)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory,
				webResourceManager);
        this.fieldMapperFactory = fieldMapperFactory;
		this.copyIssueBeanFactory = copyIssueBeanFactory;
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
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());

        CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(entityLink.getProjectKey(), getIssueObject(),
				issueType, copyComments);
        Either<NegativeResponseStatus, FieldPermissionsBean> result = proxy.checkPermissions(copyIssueBean);
        FieldPermissionsBean fieldValidationBean = handleGenericResponseStatus(proxy, result, null);
        if(fieldValidationBean == null){
            return getGenericResponseHandlerResult();
        }

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

    public boolean getCopyAttachments()
    {
        return copyAttachments;
    }

    public void setCopyAttachments(final boolean copyAttachments)
    {
        this.copyAttachments = copyAttachments;
    }

    public boolean getCopyIssueLinks()
    {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(final boolean copyIssueLinks)
    {
        this.copyIssueLinks = copyIssueLinks;
    }

	public boolean getCopyComments() {
		return copyComments;
	}

	public void setCopyComments(boolean copyComments) {
		this.copyComments = copyComments;
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
