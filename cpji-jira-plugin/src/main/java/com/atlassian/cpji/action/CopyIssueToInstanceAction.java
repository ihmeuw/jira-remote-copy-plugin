package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.CopyIssueService;
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
import com.atlassian.cpji.util.IssueLinkCopier;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.velocity.VelocityManager;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class CopyIssueToInstanceAction extends AbstractCopyIssueAction implements OperationContext {
	private String copiedIssueKey;
	private final Map fieldValuesHolder = Maps.newHashMap();
	private String issueType;
	private List<MissingFieldPermissionDescription> systemMissingFieldPermissionDescriptions;
	private List<MissingFieldPermissionDescription> customMissingFieldPermissionDescriptions;
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
	private final CopyIssueService copyIssueService;

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
			final CopyIssueBeanFactory copyIssueBeanFactory,
			final IssueTypeManager issueTypeManager,
			final CopyIssueService copyIssueService) {
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
		this.copyIssueService = copyIssueService;

		setCurrentStep("confirmation");
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
		IssueCreationResultBean copiedIssue = handleGenericResponseStatus(proxy, result,
				new Function<NegativeResponseStatus, Void>() {
					@Override
					public Void apply(@Nullable NegativeResponseStatus input) {
						addErrorCollection(CopyIssueToInstanceAction.this.processErrors(input.getErrorCollection()));
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
				Either<NegativeResponseStatus, SuccessfulResponse> addResult = proxy
						.addAttachment(copiedIssueKey, attachmentFile, attachment.getFilename(),
								attachment.getMimetype());
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
            IssueLinkCopier copier = new IssueLinkCopier(issueLinkManager, remoteIssueLinkManager, proxy);
            copier.copyLocalAndRemoteLinks(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId());
		}

		if (StringUtils.isNotBlank(remoteIssueLink)) {
			final Collection<IssueLinkType> copiedTypeCollection = issueLinkTypeManager
					.getIssueLinkTypesByName("Copied");
			if (copiedTypeCollection.size() > 0) {
				final RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);

				proxy.copyLocalIssueLink(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId(),
						Iterables.get(copiedTypeCollection, 0),
						remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.OUTWARD
								: JiraProxy.LinkCreationDirection.IGNORE,
						remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.INWARD
								: JiraProxy.LinkCreationDirection.IGNORE);
			}
		}

		linkToNewIssue = proxy.getIssueUrl(copiedIssue.getIssueKey());

		setCurrentStep("success");
		return SUCCESS;
	}

	protected ErrorCollection processErrors(ErrorCollection errorCollection) {
		Preconditions.checkNotNull(errorCollection);
		final Pattern p = Pattern.compile("(.+?): (.*)");

		// transform messages like "Environment: Environment is required." -> "Environment is required."
		errorCollection.setErrorMessages(Collections2.transform(errorCollection.getErrorMessages(), new Function<String, String>() {
			@Override
			public String apply(String input) {
				Matcher m = p.matcher(input);
				if (m.matches() && m.groupCount() == 2 && StringUtils.startsWith(m.group(2), m.group(1))) {
					return m.group(2);
				}
				return input;
			}
		}));
		return errorCollection;
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

	public Map getFieldValuesHolder() {
		return fieldValuesHolder;
	}

	@Override
	public IssueOperation getIssueOperation() {
		return IssueOperations.CREATE_ISSUE_OPERATION;
	}

	public String getHtmlForField(MissingFieldPermissionDescription permission) {
		final ImmutableSet<ValidationCode> possibleMapping = ImmutableSet.of(ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED,
				ValidationCode.FIELD_MANDATORY_VALUE_NOT_MAPPED, ValidationCode.FIELD_VALUE_NOT_MAPPED);

		if (getSelectedDestinationProject().getJiraLocation().isLocal()
				&& possibleMapping.contains(permission.getValidationCode())) {
			FieldLayoutItem fieldLayoutItem = fieldLayoutService
					.createDefaultFieldLayoutItem(permission.getFieldId(), true);
			if (fieldLayoutItem.getOrderableField() != null) {
				OrderableField orderableField = fieldLayoutItem.getOrderableField();
				Object defaultFieldValue = defaultFieldValuesManager
						.getDefaultFieldValue(getSelectedDestinationProject().getProjectKey(),
								orderableField.getId(), issueType);
				if (ActionContext.getParameters().get(orderableField.getId()) == null) {
					if (defaultFieldValue != null) {
						orderableField.populateFromParams(getFieldValuesHolder(),
								ImmutableMap.of(orderableField.getId(), defaultFieldValue));
					}
				}

				final MutableIssue fakeIssue = issueFactory.getIssue();
				fakeIssue.setProjectObject(
						projectManager.getProjectObjByKey(getSelectedDestinationProject().getProjectKey()));
				fakeIssue.setIssueTypeObject(
						copyIssueService.findIssueType(getIssueType(), fakeIssue.getProjectObject()));
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

	protected boolean canCopyField(ValidationCode validationCode) {
		switch (validationCode) {
			case OK:
				return true;
			case FIELD_NOT_MAPPED:
				return true;
			case FIELD_VALUE_NOT_MAPPED:
				return true;
			case FIELD_PERMISSION_MISSING:
				return false;
			case FIELD_MANDATORY_NO_PERMISSION:
				return false;
			case FIELD_MANDATORY_VALUE_NOT_MAPPED:
				return false || getSelectedDestinationProject().getJiraLocation().isLocal(); // for local copies we provide a way to fill missing values
			case FIELD_MANDATORY_VALUE_NOT_MAPPED_USING_DEFAULT_VALUE:
				return true;
			case FIELD_MANDATORY_NO_PERMISSION_MAPPED_USING_DEFAULT_VALUE:
				return true;
			case FIELD_MANDATORY_BUT_NOT_SUPPLIED:
				return false || getSelectedDestinationProject().getJiraLocation().isLocal(); // for local copies we provide a way to fill missing values
			case FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE:
				return true;
			default:
				throw new UnsupportedOperationException(
						String.format("Unknown validation code: %s", validationCode.toString()));
		}
	}

	@Override
	@RequiresXsrfCheck
	public String doDefault() throws Exception {
		String permissionCheck = checkPermissions();
		if (!permissionCheck.equals(SUCCESS)) {
			return permissionCheck;
		}
		final SelectedProject entityLink = getSelectedDestinationProject();
		final JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());

		CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(entityLink.getProjectKey(), getIssueObject(),
				issueType, copyComments);
		Either<NegativeResponseStatus, FieldPermissionsBean> result = proxy.checkPermissions(copyIssueBean);
		FieldPermissionsBean fieldValidationBean = handleGenericResponseStatus(proxy, result, null);
		if (fieldValidationBean == null) {
			return getGenericResponseHandlerResult();
		}

		List<SystemFieldPermissionBean> fieldPermissionBeans = fieldValidationBean.getSystemFieldPermissionBeans();
		systemMissingFieldPermissionDescriptions = new ArrayList<MissingFieldPermissionDescription>();
		canCopyIssue = true;

		final Map<String, FieldMapper> fieldMappers = fieldMapperFactory.getSystemFieldMappers();
		for (SystemFieldPermissionBean fieldPermissionBean : fieldPermissionBeans) {
			ValidationCode validationCode = ValidationCode.valueOf(fieldPermissionBean.getValidationCode());
			boolean canCopyField = canCopyField(validationCode);
			if (!canCopyField && canCopyIssue) {
				canCopyIssue = false;
			}

			FieldMapper fieldMapper = fieldMappers.get(fieldPermissionBean.getFieldId());

			if (fieldMapper != null && !ValidationCode.OK.equals(validationCode)) {
				systemMissingFieldPermissionDescriptions.add(new MissingFieldPermissionDescription(
						fieldPermissionBean,
						getI18nHelper().getText(fieldMapper.getFieldNameKey()),
						getI18nHelper().getText(validationCode.getI18nKey()), canCopyField));
			} else if (fieldMapper == null) {
				log.error("No support for field with id '" + fieldPermissionBean.getFieldId() + "'");
			}
		}

		List<CustomFieldPermissionBean> customFieldPermissionBeans = fieldValidationBean
				.getCustomFieldPermissionBeans();
		customMissingFieldPermissionDescriptions = new ArrayList<MissingFieldPermissionDescription>();
		if (customFieldPermissionBeans != null) {
			for (CustomFieldPermissionBean customFieldPermissionBean : customFieldPermissionBeans) {
				ValidationCode validationCode = ValidationCode.valueOf(customFieldPermissionBean.getValidationCode());
				boolean canCopyField = canCopyField(validationCode);
				if (!canCopyField && canCopyIssue) {
					canCopyIssue = false;
				}

				if (!ValidationCode.OK.equals(validationCode)) {
					customMissingFieldPermissionDescriptions.add(new MissingFieldPermissionDescription(
							customFieldPermissionBean,
							customFieldPermissionBean.getFieldName(),
							getI18nHelper().getText(validationCode.getI18nKey()), canCopyField));
				}
			}
		}
		return INPUT;
	}

	public boolean isCopyingPossible() {
		return canCopyIssue;
	}

	public List<MissingFieldPermissionDescription> getSystemFieldPermissions() {
		return systemMissingFieldPermissionDescriptions;
	}

	public List<MissingFieldPermissionDescription> getCustomFieldPermissions() {
		return customMissingFieldPermissionDescriptions;
	}

	public String remoteIssueLink() {
		return remoteIssueLink;
	}

}