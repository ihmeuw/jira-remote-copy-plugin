package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.FieldLayoutService;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SimplifiedIssueLinkType;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.SystemFieldMapper;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManagerImpl;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.cpji.util.IssueLinkCopier;
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
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.webresource.api.WebResourceManager;
import com.atlassian.plugins.rest.api.json.JaxbJsonMarshaller;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.util.concurrent.LazyReference;
import com.atlassian.velocity.VelocityManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.atlassian.fugue.Either;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.VelocityException;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
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
	private boolean cleanCopy;
	private String remoteIssueLink;
	private String linkToNewIssue;
    private String summary;

	private final FieldMapperFactory fieldMapperFactory;
	private final DefaultFieldValuesManagerImpl defaultFieldValuesManager;
	private final FieldLayoutService fieldLayoutService;
	private final IssueFactory issueFactory;
	private final VelocityManager velocityManager;
	private final JiraAuthenticationContext authenticationContext;
	private final IssueLinkManager issueLinkManager;
	private final RemoteIssueLinkManager remoteIssueLinkManager;
	private final CopyIssueBeanFactory copyIssueBeanFactory;
	private final CopyIssueService copyIssueService;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;
    private final SoyTemplateRendererProvider soyTemplateRendererProvider;

	private final LazyReference<List<MissingFieldPermissionDescription>> issueFieldsThatCannotBeCopied = new LazyReference<List<MissingFieldPermissionDescription>>() {
		@Override
		protected List<MissingFieldPermissionDescription> create() throws Exception {
			return ImmutableList.copyOf(
					Iterables.filter(
							Iterables.concat(systemMissingFieldPermissionDescriptions, customMissingFieldPermissionDescriptions),
							Predicates.not(MissingFieldPermissionDescription.isDestinationFieldRequired())));
		}
	};

	private final LazyReference<List<MissingFieldPermissionDescription>> destinationIssueFieldsThatAreRequired = new LazyReference<List<MissingFieldPermissionDescription>>() {
		@Override
		protected List<MissingFieldPermissionDescription> create() throws Exception {
			return ImmutableList.copyOf(
					Iterables.filter(
							Iterables.concat(systemMissingFieldPermissionDescriptions, customMissingFieldPermissionDescriptions),
							MissingFieldPermissionDescription.isDestinationFieldRequired()));
		}
	};

	public CopyIssueToInstanceAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final IssueLinkManager issueLinkManager,
            final RemoteIssueLinkManager remoteIssueLinkManager,
            final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory,
            DefaultFieldValuesManagerImpl defaultFieldValuesManager,
            FieldLayoutService fieldLayoutService,
            IssueFactory issueFactory,
            VelocityManager velocityManager,
            JiraAuthenticationContext authenticationContext,
            IssueLinkTypeManager issueLinkTypeManager,
            final WebResourceManager webResourceManager,
            FieldMapperFactory fieldMapperFactory,
            final CopyIssueBeanFactory copyIssueBeanFactory,
            final CopyIssueService copyIssueService,
            SoyTemplateRendererProvider soyTemplateRendererProvider,
            JaxbJsonMarshaller jaxbJsonMarshaller) {
		super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory,
				webResourceManager, issueLinkTypeManager);
		this.issueLinkManager = issueLinkManager;
		this.remoteIssueLinkManager = remoteIssueLinkManager;
		this.defaultFieldValuesManager = defaultFieldValuesManager;
		this.fieldLayoutService = fieldLayoutService;
		this.issueFactory = issueFactory;
		this.velocityManager = velocityManager;
		this.authenticationContext = authenticationContext;
		this.fieldMapperFactory = fieldMapperFactory;
		this.copyIssueBeanFactory = copyIssueBeanFactory;
		this.copyIssueService = copyIssueService;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
        this.soyTemplateRendererProvider = soyTemplateRendererProvider;

        webResourceManager.requireResource(PLUGIN_KEY+":copyIssueToInstanceAction");

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

		MutableIssue issueToCopy = getMutableIssue();
		CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(linkToTargetEntity.getProjectKey(), issueToCopy,
				issueType, summary, copyComments);

		if (linkToTargetEntity.getJiraLocation().isLocal()) {
			copyIssueBean.setActionParams(ActionContext.getParameters());
			copyIssueBean.setFieldValuesHolder(getFieldValuesHolder());
		}

		Either<NegativeResponseStatus, IssueCreationResultBean> result = proxy.copyIssue(copyIssueBean);
		IssueCreationResultBean copiedIssue = handleGenericResponseStatus(proxy, result,
				input -> {
                    Preconditions.checkNotNull(input);
                    if (input.getErrorCollection() != null) {
                        addErrorCollection(CopyIssueToInstanceAction.this.processCopyIssueErrors(
                                input.getErrorCollection()));
                    }
                    return null;
                });
		if (copiedIssue == null) {
            final boolean canWeEnterValues = getSelectedDestinationProject().getJiraLocation().isLocal() && getHasErrors();
			return canWeEnterValues && !cleanCopy ? doDefault() : ERROR;
		}

		copiedIssueKey = copiedIssue.getIssueKey();
		final Collection<Attachment> attachments = issueToCopy.getAttachments();
		if (getCopyAttachments() && !attachments.isEmpty()) {
			for (final Attachment attachment : attachments) {
				Either<NegativeResponseStatus, SuccessfulResponse> addResult = proxy.addAttachment(copiedIssueKey, attachment);
				if (addResult.isLeft()) {
					NegativeResponseStatus responseStatus = addResult.left().get();
					ErrorCollection ec = responseStatus.getErrorCollection();
					if (ec != null) {
						addErrorMessages(
										Lists.newArrayList(
														Iterables.transform(ec.getErrorMessages(), input -> attachment.getFilename() + ": " + input)));
					}
				}
			}
		}


        copyAndCreateIssueLinks(proxy, issueToCopy, copiedIssue);

        proxy.clearChangeHistory(copiedIssue.getIssueKey());

		linkToNewIssue = proxy.getIssueUrl(copiedIssue.getIssueKey());

		setCurrentStep("success");
		return SUCCESS;
	}


    private void copyAndCreateIssueLinks(JiraProxy proxy, MutableIssue issueToCopy, IssueCreationResultBean copiedIssue) {
        SimplifiedIssueLinkType linkType = null;
        // Try to find a defined Issue Link type for Clone
        final IssueLinkType cloneIssueLinkType = getCloneIssueLinkType();
        if (cloneIssueLinkType == null) {
            // No Cloners link type exists, fake up one
            linkType = new SimplifiedIssueLinkType("clones", "is cloned by");
        } else {
            // Use the real Cloners link type
            linkType = new SimplifiedIssueLinkType(cloneIssueLinkType);
        }

        if (getCopyIssueLinks() && issueLinkManager.isLinkingEnabled()) {
            IssueLinkCopier copier = new IssueLinkCopier(issueLinkManager, remoteIssueLinkManager, proxy, linkType);
            Either<NegativeResponseStatus, SuccessfulResponse> copierResult = copier.copyLocalAndRemoteLinks(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId());
            if (copierResult.isLeft()) {
                ErrorCollection ec = copierResult.left().get().getErrorCollection();
                if (ec != null && ec.hasAnyErrors()) {
                    addErrorCollection(ec);
                }
            }
        }

        if (StringUtils.isNotBlank(remoteIssueLink)) {
            if (cloneIssueLinkType == null) {
                //if we faked up link type - we can't use it in local clone
                if (proxy.getJiraLocation().isLocal()) {
                    linkType = null;
                }
            }

            if (linkType != null) {
                final RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);
                final JiraProxy.LinkCreationDirection localDirection, remoteDirection;
                if (remoteIssueLinkType.hasLocalIssueLinkToRemote()) {
                    localDirection = JiraProxy.LinkCreationDirection.INWARD;
                } else {
                    localDirection = JiraProxy.LinkCreationDirection.IGNORE;
                }

                if(remoteIssueLinkType.hasRemoteIssueLinkToLocal()){
                    remoteDirection = JiraProxy.LinkCreationDirection.OUTWARD;
                } else {
                    remoteDirection = JiraProxy.LinkCreationDirection.IGNORE;
                }

                proxy.copyLocalIssueLink(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId(),
                        linkType,
                        localDirection,
                        remoteDirection);
            }
        }
    }

	protected ErrorCollection processCopyIssueErrors(ErrorCollection errorCollection) {
		Preconditions.checkNotNull(errorCollection);
		final Pattern p = Pattern.compile("(.+?): (.*)");

		// transform messages like "Environment: Environment is required." -> "Environment is required."
		errorCollection.setErrorMessages(Collections2.transform(errorCollection.getErrorMessages(), input -> {
            Matcher m = p.matcher(input);
            if (m.matches() && m.groupCount() == 2 && StringUtils.startsWith(m.group(2), m.group(1))) {
                return m.group(2);
            }
            return input;
        }));

		return errorCollection;
	}


	public String getLinkToNewIssue() {
		return linkToNewIssue;
	}


	public String getCopiedIssueKey() {
		return copiedIssueKey;
	}

	public boolean isCleanCopy() {
		return cleanCopy;
	}

	public void setCleanCopy(boolean cleanCopy) {
		this.cleanCopy = cleanCopy;
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


	public String getFieldHtml(MissingFieldPermissionDescription permission) throws SoyException {
		final ImmutableSet<ValidationCode> possibleMapping = ImmutableSet.of(ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED,
				ValidationCode.FIELD_MANDATORY_VALUE_NOT_MAPPED, ValidationCode.FIELD_VALUE_NOT_MAPPED);

		if (getSelectedDestinationProject().getJiraLocation().isLocal()
				&& possibleMapping.contains(permission.getValidationCode())) {
			FieldLayoutItem fieldLayoutItem = fieldLayoutService
					.createDefaultFieldLayoutItem(permission.getFieldId(), true);
			if (fieldLayoutItem.getOrderableField() != null) {
				OrderableField orderableField = fieldLayoutItem.getOrderableField();
				String[] defaultFieldValue = defaultFieldValuesManager
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

                final String fieldEditHtml = orderableField.getEditHtml(fieldLayoutItem, this, this, fakeIssue, RequiredFieldsAwareAction
                        .getDisplayParameters());
                final String unmappedValues;
                if(!permission.getUnmappedValues().isEmpty()){
                    unmappedValues =  jaxbJsonMarshaller.marshal(permission.getUnmappedValues());
                } else {
                    unmappedValues = "";
                }
                final String unmappedValuesHTML = soyTemplateRendererProvider.getRenderer().render(PLUGIN_KEY+":copyIssueToInstanceAction",
                    "RIC.Templates.jsonData",
                    ImmutableMap.<String, Object>of("json", unmappedValues,  "fieldId", orderableField.getId())
                );
                return fieldEditHtml +unmappedValuesHTML;
			}
		}

		try {
			return velocityManager.getEncodedBody("com/atlassian/cpji/templates/", "default.html.for.field.vm",
					getApplicationProperties().getEncoding(),
					JiraVelocityUtils
							.getDefaultVelocityParams(ImmutableMap.<String, Object>of("permission", permission, "i18n", getI18nHelper()),
									authenticationContext));
		} catch (VelocityException e) {
			throw new RuntimeException(String.format("Unable to render template"), e);
		}
	}

	protected boolean canCopyField(ValidationCode validationCode) {
		switch (validationCode) {
			case OK:
				return true;
			case FIELD_TYPE_NOT_SUPPORTED:
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
			case FIELD_PERMISSION_MISSING_USING_DEFAULT_VALUE:
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

		CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(entityLink.getProjectKey(), getMutableIssue(),
				issueType, summary,copyComments);
		Either<NegativeResponseStatus, FieldPermissionsBean> result = proxy.checkPermissions(copyIssueBean);
		FieldPermissionsBean fieldValidationBean = handleGenericResponseStatus(proxy, result, null);
		if (fieldValidationBean == null) {
			return getGenericResponseHandlerResult();
		}

		List<SystemFieldPermissionBean> fieldPermissionBeans = fieldValidationBean.getSystemFieldPermissionBeans();
		systemMissingFieldPermissionDescriptions = new ArrayList<MissingFieldPermissionDescription>();
		canCopyIssue = true;

		final Map<String, SystemFieldMapper> fieldMappers = fieldMapperFactory.getSystemFieldMappers();
		for (SystemFieldPermissionBean fieldPermissionBean : fieldPermissionBeans) {
			ValidationCode validationCode = ValidationCode.valueOf(fieldPermissionBean.getValidationCode());
			boolean canCopyField = canCopyField(validationCode);
			if (!canCopyField && canCopyIssue) {
				canCopyIssue = false;
			}

			SystemFieldMapper systemFieldMapper = fieldMappers.get(fieldPermissionBean.getFieldId());

			if (systemFieldMapper != null && !ValidationCode.OK.equals(validationCode)) {
				systemMissingFieldPermissionDescriptions.add(new MissingFieldPermissionDescription(
						fieldPermissionBean,
						getI18nHelper().getText(systemFieldMapper.getFieldNameKey()),
						getI18nHelper().getText(validationCode.getI18nKey()), canCopyField));
			} else if (systemFieldMapper == null) {
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
		if (!copyIssueBean.getUnsupportedCustomFields().isEmpty()) {
			customMissingFieldPermissionDescriptions.addAll(Collections2.transform(copyIssueBean.getUnsupportedCustomFields(), customFieldPermissionBean -> {
                Preconditions.checkNotNull(customFieldPermissionBean);
                ValidationCode validationCode = ValidationCode
                        .valueOf(customFieldPermissionBean.getValidationCode());
                return new MissingFieldPermissionDescription(
                        customFieldPermissionBean,
                        customFieldPermissionBean.getFieldName(),
                        getI18nHelper().getText(validationCode.getI18nKey()), canCopyField(validationCode));
            }));
		}
		return INPUT;
	}

    public String getWarningMarkerHtml() throws SoyException{
        return soyTemplateRendererProvider.getRenderer().render(PLUGIN_KEY+":copyIssueToInstanceAction",
                "RIC.Templates.warningMarker", ImmutableMap.<String, Object>of());
    }

    public boolean isCopyingPossible() {
		return canCopyIssue;
	}

	public List<MissingFieldPermissionDescription> getIssueFieldsThatCannotBeCopied() {
		return issueFieldsThatCannotBeCopied.get();
	}

	public List<MissingFieldPermissionDescription> getDestinationIssueFieldsThatAreRequired() {
		return destinationIssueFieldsThatAreRequired.get();
	}

	public String remoteIssueLink() {
		return remoteIssueLink;
	}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

}
