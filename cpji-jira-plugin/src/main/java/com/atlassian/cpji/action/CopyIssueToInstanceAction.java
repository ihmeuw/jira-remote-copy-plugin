package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.FieldLayoutService;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.velocity.VelocityManager;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.VelocityException;
import webwork.action.ActionContext;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
public class CopyIssueToInstanceAction extends AbstractCopyIssueAction implements OperationContext {
    private String copiedIssueKey;
	private final Map fieldValuesHolder = Maps.newHashMap();
	private String issueType;
	private List<FieldPermission> systemFieldPermissions;
	private List<FieldPermission> customFieldPermissions;
	private boolean canCopyIssue = true;
	private boolean copyAttachments;
	private boolean copyIssueLinks;
	private boolean copyComments;
	private String remoteIssueLink;
	private String linkToNewIssue;

	private final FieldMapperFactory fieldMapperFactory;
	private final DefaultFieldValuesManager defaultFieldValuesManager;
	private final FieldLayoutService fieldLayoutService;
	private final IssueFactory issueFactory;
	private final VelocityManager velocityManager;
	private final JiraAuthenticationContext authenticationContext;
	private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final CopyIssueBeanFactory copyIssueBeanFactory;


    public CopyIssueToInstanceAction(
			final SubTaskManager subTaskManager,
			final FieldLayoutManager fieldLayoutManager,
			final CommentManager commentManager,
			final CopyIssuePermissionManager copyIssuePermissionManager,
			final IssueLinkManager issueLinkManager,
			final RemoteIssueLinkManager remoteIssueLinkManager,
			final ApplicationLinkService applicationLinkService,
			final JiraProxyFactory jiraProxyFactory,
			DefaultFieldValuesManager defaultFieldValuesManager,
			FieldLayoutService fieldLayoutService,
			IssueFactory issueFactory,
			VelocityManager velocityManager,
			JiraAuthenticationContext authenticationContext,
			IssueLinkTypeManager issueLinkTypeManager,
			final WebResourceManager webResourceManager,
			FieldMapperFactory fieldMapperFactory,
			final CopyIssueBeanFactory copyIssueBeanFactory) {
        super(subTaskManager, fieldLayoutManager, commentManager,
                copyIssuePermissionManager, applicationLinkService, jiraProxyFactory,
                webResourceManager);
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
		this.defaultFieldValuesManager = defaultFieldValuesManager;
		this.fieldLayoutService = fieldLayoutService;
		this.issueFactory = issueFactory;
		this.velocityManager = velocityManager;
		this.authenticationContext = authenticationContext;
		this.issueLinkTypeManager = issueLinkTypeManager;
		this.fieldMapperFactory = fieldMapperFactory;
		this.copyIssueBeanFactory = copyIssueBeanFactory;
    }

	@Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS)) {
            return permissionCheck;
        }

        final SelectedProject linkToTargetEntity = getSelectedDestinationProject();
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(linkToTargetEntity.getJiraLocation());

        MutableIssue issueToCopy = getIssueObject();
        CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(linkToTargetEntity.getProjectKey(), issueToCopy,
                issueType, copyComments);

		if (linkToTargetEntity.getJiraLocation().isLocal()) {
			copyIssueBean.setActionParams(ActionContext.getParameters());
			copyIssueBean.setFieldValuesHolder(getFieldValuesHolder());
		}

		Either<NegativeResponseStatus, IssueCreationResultBean> result = proxy.copyIssue(copyIssueBean);
        IssueCreationResultBean copiedIssue = handleGenericResponseStatus(proxy, result, new Function<NegativeResponseStatus, Void>() {
			@Override
			public Void apply(@Nullable NegativeResponseStatus input) {
				addErrorCollection(input.getErrorCollection());
				return null;
			}
		});
        if (copiedIssue == null) {
            return getSelectedDestinationProject().getJiraLocation().isLocal() && getHasErrors() ? doDefault() : ERROR;
        }

        copiedIssueKey = copiedIssue.getIssueKey();
        final Collection<Attachment> attachments = issueToCopy.getAttachments();
        if (getCopyAttachments() && !attachments.isEmpty()) {
            for (final Attachment attachment : attachments) {
                File attachmentFile = AttachmentUtils.getAttachmentFile(attachment);
                Either<NegativeResponseStatus, SuccessfulResponse> addResult = proxy.addAttachment(copiedIssueKey, attachmentFile, attachment.getFilename(), attachment.getMimetype());
                if (addResult.isLeft()) {
                    NegativeResponseStatus responseStatus = addResult.left().get();
                    ErrorCollection ec = responseStatus.getErrorCollection();
                    if (ec != null) {
                        addErrorMessages(
                                Lists.newArrayList(
                                        Iterables.transform(ec.getErrorMessages(), new Function<String, String>() {
                                            @Override
                                            public String apply(@Nullable String input) {
                                                return attachment.getFilename() + ": " + input;
                                            }
                                        })));
                    }
                }
            }
        }

        if (getCopyIssueLinks() && issueLinkManager.isLinkingEnabled()) {
            copyLocalIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId());
            copyRemoteIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey());
            proxy.convertRemoteIssueLinksIntoLocal(copiedIssueKey);
        }

        if (StringUtils.isNotBlank(remoteIssueLink)) {
            final Collection<IssueLinkType> copiedTypeCollection = issueLinkTypeManager.getIssueLinkTypesByName("Copied");
            if (copiedTypeCollection.size() > 0) {
                final RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);

                proxy.copyLocalIssueLink(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId(),
                        Iterables.get(copiedTypeCollection, 0),
                        remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.OUTWARD : JiraProxy.LinkCreationDirection.IGNORE,
                        remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.INWARD : JiraProxy.LinkCreationDirection.IGNORE);
            }
        }

        linkToNewIssue = proxy.getIssueUrl(copiedIssue.getIssueKey());

        return SUCCESS;
    }


    private void copyLocalIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) throws ResponseException, CredentialsRequiredException {
        for (final IssueLink inwardLink : issueLinkManager.getInwardLinks(localIssue.getId())) {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId, type, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        }
        for (final IssueLink outwardLink : issueLinkManager.getOutwardLinks(localIssue.getId())) {

            final IssueLinkType type = outwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type, JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        }
    }

    private void copyRemoteIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey) throws ResponseException, CredentialsRequiredException {
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue)) {
            remoteJira.copyRemoteIssueLink(remoteIssueLink, copiedIssueKey);
        }
    }

    public String getLinkToNewIssue() {
        return linkToNewIssue;
    }


    public String getCopiedIssueKey() {
        return copiedIssueKey;
    }

    public boolean getCopyAttachments() {
        return copyAttachments;
    }

    public void setCopyAttachments(final boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    public boolean getCopyComments() {
        return copyComments;
    }

    public void setCopyComments(boolean copyComments) {
        this.copyComments = copyComments;
    }

    public boolean getCopyIssueLinks() {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(final boolean copyIssueLinks) {
        this.copyIssueLinks = copyIssueLinks;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(final String issueType) {
        this.issueType = issueType;
    }

    public String getRemoteIssueLink() {
        return remoteIssueLink;
    }

    public void setRemoteIssueLink(final String remoteIssueLink) {
        this.remoteIssueLink = remoteIssueLink;
    }

	public Map getFieldValuesHolder()
	{
		return fieldValuesHolder;
	}

	@Override
	public IssueOperation getIssueOperation() {
		return IssueOperations.CREATE_ISSUE_OPERATION;
	}

	public String getHtmlForField(FieldPermission permission)
	{
		if (getSelectedDestinationProject().getJiraLocation().isLocal()) {
			FieldLayoutItem fieldLayoutItem = fieldLayoutService.createDefaultFieldLayoutItem(permission.getFieldId(), true);
			if (fieldLayoutItem.getOrderableField() != null) {
				OrderableField orderableField = fieldLayoutItem.getOrderableField();
				Object defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(getSelectedDestinationProject().getProjectKey(),
						orderableField.getId(), issueType);
				if (ActionContext.getParameters().get(orderableField.getId()) == null)
				{
					if (defaultFieldValue != null)
					{
						orderableField.populateFromParams(getFieldValuesHolder(), ImmutableMap.of(orderableField.getId(), defaultFieldValue));
					}
				}

				final MutableIssue fakeIssue = issueFactory.getIssue();
				fakeIssue.setProjectObject(projectManager.getProjectObjByKey(getSelectedDestinationProject().getProjectKey()));
				return orderableField.getEditHtml(fieldLayoutItem, this, this, fakeIssue, RequiredFieldsAwareAction
						.getDisplayParameters());
			}
		}

		try {
			return velocityManager.getEncodedBody("com/atlassian/cpji/templates/", "default.html.for.field.vm",
					getApplicationProperties().getEncoding(),
					JiraVelocityUtils
							.getDefaultVelocityParams(ImmutableMap.<String, Object>of("permission", permission),
									authenticationContext));
		} catch (VelocityException e) {
			throw new RuntimeException(String.format("Unable to render template"), e);
		}
	}

	@Override
	@RequiresXsrfCheck
	public String doDefault() throws Exception
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
		return INPUT;
	}

	public boolean isCopyingPossible()
	{
		return canCopyIssue || getSelectedDestinationProject().getJiraLocation().isLocal();
	}

	public List<FieldPermission> getSystemFieldPermissions()
	{
		return systemFieldPermissions;
	}

	public List<FieldPermission> getCustomFieldPermissions()
	{
		return customFieldPermissions;
	}

	public String remoteIssueLink()
	{
		return remoteIssueLink;
	}

}