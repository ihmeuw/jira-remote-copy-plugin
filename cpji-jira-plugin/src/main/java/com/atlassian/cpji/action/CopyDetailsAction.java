package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.core.util.FileSize;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.cpji.util.IssueLinkCopier;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.UrlBuilder;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.util.concurrent.LazyReference;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.opensymphony.util.TextUtils;
import io.atlassian.fugue.Either;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @since v1.4
 */
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
@SupportedMethods({RequestMethod.GET})
public class CopyDetailsAction extends AbstractCopyIssueAction implements OperationContext
{
    private String remoteUserName;
    private String remoteFullUserName;
    private String summary;

    private String issueType;
    private boolean copyComments;
    private boolean copyAttachments;
    private boolean copyIssueLinks;
    private String remoteIssueLink;

    private Collection<Option> availableIssueTypes;
    private Collection<Option> availableSubTaskTypes;
	private final IssueLinkManager issueLinkManager;
    private final ApplicationProperties applicationProperties;
    private final FieldManager fieldManager;
	private final RemoteIssueLinkManager remoteIssueLinkManager;

	private CopyInformationBean copyInfo;

	public CopyDetailsAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory,
            final WebResourceManager webResourceManager,
            final IssueLinkManager issueLinkManager,
            final IssueLinkTypeManager issueLinkTypeManager,
            final ApplicationProperties applicationProperties,
            final FieldManager fieldManager,
			final RemoteIssueLinkManager remoteIssueLinkManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory, webResourceManager, issueLinkTypeManager);
		this.issueLinkManager = issueLinkManager;
        this.applicationProperties = applicationProperties;
        this.fieldManager = fieldManager;
		this.remoteIssueLinkManager = remoteIssueLinkManager;

		setCurrentStep("copydetails");
        webResourceManager.requireResource(PLUGIN_KEY+":copyDetailsAction");
	}

	public boolean isIssueWithComments() {
		return !commentManager.getComments(getIssueObject()).isEmpty();
	}

	public boolean isIssueWithAttachments() {
		return !getIssueObject().getAttachments().isEmpty();
	}

	public boolean isIssueWithLinks() {
		final MutableIssue issue = getMutableIssue();
		if (issueLinkManager.isLinkingEnabled()) {
			//checking if there are any not-subtask issue links (inward or outward)
			return Iterables.any(issueLinkManager.getOutwardLinks(issue.getId()), IssueLinkCopier.isNotSubtaskIssueLink)
					|| Iterables.any(issueLinkManager.getInwardLinks(issue.getId()), IssueLinkCopier.isNotSubtaskIssueLink)
					|| !remoteIssueLinkManager.getRemoteIssueLinksForIssue(issue).isEmpty();
		}
		return false;
	}

    private void validateSummary() {
        final OrderableField summaryField = fieldManager.getOrderableField("summary");
        final Map<String, String> values = ImmutableMap.of(summaryField.getId(), summary);
        final SimpleErrorCollection ec = new SimpleErrorCollection();
        OperationContext isolatedContext = new OperationContext(){
            @Override
            public Map getFieldValuesHolder() {
                return values;
            }

            @Override
            public IssueOperation getIssueOperation() {
                return IssueOperations.CREATE_ISSUE_OPERATION;
            }
        };
        summaryField.validateParams(isolatedContext, ec, getI18nHelper(), null, null);
        if(ec.getErrors().containsKey(summaryField.getId())){
            addError("summary", ec.getErrors().get(summaryField.getId()));
        }
    }

    protected String doExecute() throws Exception {

        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }

        validateSummary();
        if(hasAnyErrors()) {
            if (!initParams()) {
                return getGenericResponseHandlerResult();
            }
            return INPUT;
        } else {

            UrlBuilder builder = new UrlBuilder("CopyIssueToInstanceAction!default.jspa")
                    .addParameter("id", getId())
                    .addParameter("issueType", issueType)
                    .addParameter("summary", summary)
                    .addParameter("copyComments", copyComments)
                    .addParameter("copyAttachments", copyAttachments)
                    .addParameter("copyIssueLinks", copyIssueLinks)
                    .addParameter("remoteIssueLink", remoteIssueLink)
                    .addParameter("targetEntityLink", targetEntityLink)
                    .addParameter("atl_token", getXsrfToken());
            return getRedirect(builder.asUrlString(), true);
        }

    }

    @Override
    public String doDefault() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }

        if (!initParams()) {
            return getGenericResponseHandlerResult();
        }

        //setting new issue object's summary
        String clonePrefixProperties = applicationProperties.getDefaultBackedString(APKeys.JIRA_CLONE_PREFIX);
        final String clonePrefix = clonePrefixProperties + (Strings.isNullOrEmpty(clonePrefixProperties) ? "" : " ");
        summary = clonePrefix + getIssueObject().getSummary();

        copyComments = true;
        copyAttachments = true;
        copyIssueLinks = true;

        return INPUT;
    }

    private boolean initParams() {
        SelectedProject entityLink = getSelectedDestinationProject();

        JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());
        Either<NegativeResponseStatus, CopyInformationBean> response = proxy.getCopyInformation(
				entityLink.getProjectKey());
        copyInfo = handleGenericResponseStatus(proxy, response, null);
        if(copyInfo == null){
            return false;
        }

        availableIssueTypes = getIssueTypeOptionsList(copyInfo.getIssueTypes());

        // if we copy to the same project display also subtask types
        if(proxy.getJiraLocation().isLocal() && getIssueObject().getProjectObject().getKey().equals(entityLink.getProjectKey())
				&& getIssueObject().getParentId() != null) {
            availableSubTaskTypes = getIssueTypeOptionsList(copyInfo.getSubtaskIssueTypes());
        } else {
            availableSubTaskTypes = Collections.emptyList();
        }

        UserBean user = copyInfo.getRemoteUser();
        remoteUserName = user.getUserName();
        remoteFullUserName = copyInfo.getRemoteUser().getFullName();
        return true;
    }

    public String getRemoteUserName()
    {
        return remoteUserName;
    }

    public String getRemoteFullUserName()
    {
        return remoteFullUserName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isCopyComments() {
        return copyComments;
    }

    public void setCopyComments(boolean copyComments) {
        this.copyComments = copyComments;
    }

    public boolean isCopyAttachments() {
        return copyAttachments;
    }

    public void setCopyAttachments(boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    public boolean isCopyIssueLinks() {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(boolean copyIssueLinks) {
        this.copyIssueLinks = copyIssueLinks;
    }

    public String getRemoteIssueLink() {
        return remoteIssueLink;
    }

    public void setRemoteIssueLink(String remoteIssueLink) {
        this.remoteIssueLink = remoteIssueLink;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public boolean attachmentsEnabled()
    {
        return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
    }

	public boolean linksEnabled()
	{
		return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ISSUELINKING);
	}

	private List<Option> getIssueTypeOptionsList(final Collection<IssueTypeBean> values)
    {
        MutableIssue issue = getMutableIssue();
        List<Option> result = Lists.newArrayList();
        if(values != null){
        for (IssueTypeBean value : values)
            {
                final boolean isSelected = StringUtils.isNotEmpty(issueType) ?
                        StringUtils.equalsIgnoreCase(issueType, value.getName()) :
                        StringUtils.equalsIgnoreCase(value.getName(), issue.getIssueTypeObject().getName());
                result.add(new Option(value.getName(), isSelected));
            }
        }
        return result;
    }

    public List<Option> getIssueLinkOptions()
    {
		List<Option> issueLinkOptions = Lists.newArrayList();

		if (linksEnabled() && copyInfo.getIssueLinkingEnabled()) {
			issueLinkOptions.add(new Option(RemoteIssueLinkType.RECIPROCAL.name(), false, getText(RemoteIssueLinkType.RECIPROCAL.getI18nKey())));
		}

        // JRADEV-18180 we can create one-way links only for non-local destinations
        if(!getSelectedDestinationProject().getJiraLocation().isLocal()){
            if (copyInfo.getIssueLinkingEnabled() && copyInfo.getHasCreateLinksPermission()) {
                issueLinkOptions.add(new Option(RemoteIssueLinkType.INCOMING.name(), false, getText(RemoteIssueLinkType.INCOMING.getI18nKey())));
            }
            if (linksEnabled()) {
                issueLinkOptions.add(new Option(RemoteIssueLinkType.OUTGOING.name(), false, getText(RemoteIssueLinkType.OUTGOING.getI18nKey())));
            }
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
				|| isCopyCommentsSectionVisible();
	}

	public boolean isCopyAttachmentsSectionVisible() {
		return attachmentsEnabled() && isIssueWithAttachments();
	}

	public boolean isCopyIssueLinksSectionVisible() {
		return linksEnabled() && isIssueWithLinks();
	}

	public boolean isCreateIssueLinkSectionVisible() {
		return (linksEnabled() || copyInfo.getIssueLinkingEnabled()) &&
                // For a local link we require the "Cloners" link type to exist
                (!getSelectedDestinationProject().getJiraLocation().isLocal() || getCloneIssueLinkType() != null);
	}

	public boolean isCopyCommentsSectionVisible() {
		return isIssueWithComments();
	}

    private LazyReference<List<Attachment>> attachmentsLargerThanAllowed = new LazyReference<List<Attachment>>() {
        @Override
        protected List<Attachment> create() throws Exception {
            if(!canRemoteServerRecieveAttachments()){
                return Collections.emptyList();
            }
            return ImmutableList.copyOf(
                    Iterables.filter(getIssueObject().getAttachments(), new Predicate<Attachment>() {
                        @Override
                        public boolean apply(Attachment input) {
                            return input.getFilesize() > copyInfo.getMaxAttachmentSize();
                        }
                    })
            );
        }
    };

    public List<Attachment> getAttachmentsLargerThanAllowed(){
        return attachmentsLargerThanAllowed.get();
    }

    private int getAllAttachmentsCount(){
        return getIssueObject().getAttachments().size();
    }

    public boolean isCopyAttachmentsEnabled(){
        return canRemoteServerRecieveAttachments()
                && getAttachmentsLargerThanAllowed().size() < getAllAttachmentsCount();
    }

    private boolean canRemoteServerRecieveAttachments() {
        return copyInfo.getAttachmentsEnabled() && copyInfo.getHasCreateAttachmentPermission();
    }

    public String getCopyAttachmentsErrorMessageHtml() {
		if (!copyInfo.getAttachmentsEnabled()) {
			return getText("cpji.attachments.are.disabled");
		} else if(!copyInfo.getHasCreateAttachmentPermission()) {
			return getText("cpji.not.permitted.to.create.attachments");
		} else{
            List<Attachment> attachments = getAttachmentsLargerThanAllowed();
            if(attachments.size() == getAllAttachmentsCount()){
                return getText("cpji.all.attachments.are.too.big", FileSize.format(copyInfo.getMaxAttachmentSize()));
            } else if(attachments.size() == 1) {
                return getText("cpji.one.attachment.is.too.big", TextUtils.htmlEncode(attachments.get(0).getFilename()), FileSize.format(copyInfo.getMaxAttachmentSize()));
            }else if(attachments.size() > 1){
                String countText = getText("cpji.some.attachments.count", Integer.toString(attachments.size()));
                return getText("cpji.some.attachments.are.too.big", countText, FileSize.format(copyInfo.getMaxAttachmentSize()));
            }
        }
		return "";
	}

	public String getCopyIssueLinksErrorMessage() {
		if (!copyInfo.getIssueLinkingEnabled()) {
			return getText("cpji.remote.issue.linking.is.disabled");
		} else if (!copyInfo.getHasCreateLinksPermission()) {
			return getText("cpji.not.permitted.to.link.issues");
		}
		return "";
	}

    public String getCreateIssueLinkWarningMessage() {
        if (getCloneIssueLinkType() == null)
            return getText("cloneissue.linktype.does.not.exist", getCloneIssueLinkTypeName());
        return null;
    }

	public String getCopyCommentsErrorMessage() {
		if (!copyInfo.getHasCreateCommentPermission()) {
			return getText("cpji.not.permitted.to.create.comments");
		}
		return "";
	}

	@Override
	public Map getFieldValuesHolder() {
		return Maps.newHashMap();
	}

	@Override
	public IssueOperation getIssueOperation() {
		return IssueOperations.EDIT_ISSUE_OPERATION;
	}

    public Collection<Option> getAvailableSubTaskTypes() {
        return availableSubTaskTypes;
    }
}
