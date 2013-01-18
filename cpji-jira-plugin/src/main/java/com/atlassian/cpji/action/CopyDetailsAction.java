package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.cpji.util.IssueLinkCopier;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @since v1.4
 */
public class CopyDetailsAction extends AbstractCopyIssueAction implements OperationContext
{
    private String remoteUserName;
    private String remoteFullUserName;
    private String summary;

    private Collection<Option> availableIssueTypes;
    private Collection<Option> availableSubTaskTypes;
	private final IssueLinkManager issueLinkManager;
	private final IssueLinkTypeManager issueLinkTypeManager;

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
			final IssueLinkTypeManager issueLinkTypeManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory, webResourceManager);
		this.issueLinkManager = issueLinkManager;
		this.issueLinkTypeManager = issueLinkTypeManager;

		setCurrentStep("copydetails");
        webResourceManager.requireResource(PLUGIN_KEY+":copyDetailsAction");
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
                //checking if there are any not-subtask issue links (inward or outward)
                return Iterables.any(issueLinkManager.getOutwardLinks(issue.getId()), IssueLinkCopier.isNotSubtaskIssueLink)
                        || Iterables.any(issueLinkManager.getInwardLinks(issue.getId()), IssueLinkCopier.isNotSubtaskIssueLink);
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

        JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());
        Either<NegativeResponseStatus, CopyInformationBean> response = proxy.getCopyInformation(
				entityLink.getProjectKey());
        copyInfo = handleGenericResponseStatus(proxy, response, null);
        if(copyInfo == null){
            return getGenericResponseHandlerResult();
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
        summary = getIssueObject().getSummary();
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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
        MutableIssue issue = getIssueObject();
        List<Option> result = Lists.newArrayList();
        if(values != null){
        for (IssueTypeBean value : values)
            {
                if (StringUtils.equalsIgnoreCase(value.getName(), issue.getIssueTypeObject().getName()))
                {
                    result.add(new Option(value.getName(), true));
                }
                else
                {
                    result.add(new Option(value.getName(), false));
                }
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
		if (copyInfo.getIssueLinkingEnabled() && copyInfo.getHasCreateLinksPermission()) {
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
                (!getSelectedDestinationProject().getJiraLocation().isLocal() || !issueLinkTypeManager.getIssueLinkTypesByName(CLONE_LINK_TYPE).isEmpty());
	}

	public boolean isCopyCommentsSectionVisible() {
		return isIssueWithComments();
	}

    private int getNumberOfAttachmentsLargerThanAllowed(){
        return Iterables.size(
            Iterables.filter(getIssueObject().getAttachments(), new Predicate<Attachment>() {
                @Override
                public boolean apply(@Nullable Attachment input) {
                    return input.getFilesize() > copyInfo.getMaxAttachmentSize();
                }
            })
        );
    }

    private int getAllAttachmentsCount(){
        return getIssueObject().getAttachments().size();
    }

    public boolean isCopyAttachmentsEnabled(){
        return copyInfo.getAttachmentsEnabled() && copyInfo.getHasCreateAttachmentPermission()
                && getNumberOfAttachmentsLargerThanAllowed() < getAllAttachmentsCount();
    }

	public String getCopyAttachmentsErrorMessage() {
		if (!copyInfo.getAttachmentsEnabled()) {
			return getText("cpji.attachments.are.disabled");
		} else if(!copyInfo.getHasCreateAttachmentPermission()) {
			return getText("cpji.not.permitted.to.create.attachments");
		} else{
            int count = getNumberOfAttachmentsLargerThanAllowed();
            if(count == getAllAttachmentsCount()){
                return getText("cpji.all.attachments.are.too.big");
            } else if(count > 0){
                return getText("cpji.some.attachments.are.too.big");
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
