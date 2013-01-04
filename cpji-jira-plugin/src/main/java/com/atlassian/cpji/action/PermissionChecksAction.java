package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.FieldLayoutService;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since v1.4
 */
public class PermissionChecksAction extends AbstractCopyIssueAction implements OperationContext {
	private final FieldMapperFactory fieldMapperFactory;
	private final CopyIssueBeanFactory copyIssueBeanFactory;
	private final DefaultFieldValuesManager defaultFieldValuesManager;
	private final FieldLayoutService fieldLayoutService;
	private final IssueFactory issueFactory;
	private final Map fieldValuesHolder = Maps.newHashMap();

    private String issueType;
    private List<FieldPermission> systemFieldPermissions;
    private List<FieldPermission> customFieldPermissions;
    private boolean canCopyIssue = true;
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
			final CopyIssueBeanFactory copyIssueBeanFactory,
			final DefaultFieldValuesManager defaultFieldValuesManager,
			final FieldLayoutService fieldLayoutService,
			final IssueFactory issueFactory)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory,
				webResourceManager);
        this.fieldMapperFactory = fieldMapperFactory;
		this.copyIssueBeanFactory = copyIssueBeanFactory;
		this.defaultFieldValuesManager = defaultFieldValuesManager;
		this.fieldLayoutService = fieldLayoutService;
		this.issueFactory = issueFactory;
	}

    public class FieldPermission
    {
		private final String fieldId;
		private final String fieldName;
        private final String validationMessage;
        private final boolean canCopyIssue;

        public FieldPermission(String fieldId, String name, String validationMessage, final boolean canCopyIssue)
        {
			this.fieldId = fieldId;
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

		public String getFieldId() {
			return fieldId;
		}
	}

	public Map getFieldValuesHolder()
	{
		return fieldValuesHolder;
	}

	@Override
	public IssueOperation getIssueOperation() {
		return IssueOperations.CREATE_ISSUE_OPERATION;
	}

	public String getHtmlForField(FieldLayoutItem fieldLayoutItem)
	{
		OrderableField orderableField = fieldLayoutItem.getOrderableField();
		Object defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(getSelectedDestinationProject().getProjectKey(),
				orderableField.getId(), issueType);
		if (ActionContext.getParameters().get(orderableField.getId()) == null)
		{
			if (defaultFieldValue != null)
			{
				Map actionParams = new HashMap();
				actionParams.put(orderableField.getId(), defaultFieldValue);
				orderableField.populateFromParams(getFieldValuesHolder(), actionParams);
			}
		}

		final MutableIssue fakeIssue = issueFactory.getIssue();
		fakeIssue.setProjectObject(projectManager.getProjectObjByKey(getSelectedDestinationProject().getProjectKey()));
		return orderableField.getEditHtml(fieldLayoutItem, this, this, fakeIssue, RequiredFieldsAwareAction.getDisplayParameters());
	}

	public List<FieldLayoutItem> getFieldLayoutItems() {
		return Lists.newArrayList(Iterables.filter(
				Iterables.transform(systemFieldPermissions, new Function<FieldPermission, FieldLayoutItem>() {
					@Override
					public FieldLayoutItem apply(FieldPermission input) {
						return fieldLayoutService.createDefaultFieldLayoutItem(input.getFieldId(), true);
					}
				}), new Predicate<FieldLayoutItem>() {
			@Override
			public boolean apply(FieldLayoutItem input) {
				return input.getOrderableField() != null;
			}
		}));
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
        if(fieldValidationBean == null) {
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
                systemFieldPermissions.add(new FieldPermission(
						fieldPermissionBean.getFieldId(),
						getI18nHelper().getText(fieldMapper.getFieldNameKey()),
						getI18nHelper().getText(validationCode.getI18nKey()), validationCode.canCopyIssue()));
            }
            else if (fieldMapper == null)
            {
                log.error("No support for field with id '"+ fieldPermissionBean.getFieldId() + "'");
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
                    customFieldPermissions.add(new FieldPermission(
							customFieldPermissionBean.getFieldId(),
							customFieldPermissionBean.getFieldName(),
							getI18nHelper().getText(validationCode.getI18nKey()), validationCode.canCopyIssue()));
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
