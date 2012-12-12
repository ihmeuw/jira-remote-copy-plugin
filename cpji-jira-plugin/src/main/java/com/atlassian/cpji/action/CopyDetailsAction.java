package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.FieldLayoutService;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueFieldBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import webwork.action.ActionContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.cpji.action.admin.RequiredFieldsAwareAction.getDisplayParameters;

/**
 *
 * @since v1.4
 */
public class CopyDetailsAction extends AbstractCopyIssueAction implements OperationContext
{
    private String remoteUserName;
    private String remoteFullUserName;

    private Collection<Option> availableIssueTypes;
	private final IssueLinkManager issueLinkManager;
	private final IssueLinkTypeManager issueLinkTypeManager;
	private final FieldLayoutService fieldLayoutService;
	private final DefaultFieldValuesManager defaultFieldValuesManager;

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
			final FieldLayoutService fieldLayoutService,
			final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory, webResourceManager);
		this.issueLinkManager = issueLinkManager;
		this.issueLinkTypeManager = issueLinkTypeManager;
		this.fieldLayoutService = fieldLayoutService;
		this.defaultFieldValuesManager = defaultFieldValuesManager;
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

        JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());
        Either<NegativeResponseStatus, CopyInformationBean> response = proxy.getCopyInformation(
				entityLink.getProjectKey());
        copyInfo = handleGenericResponseStatus(proxy, response, null);
        if(copyInfo == null){
            return getGenericResponseHandlerResult();
        }

        if (!copyInfo.getHasCreateIssuePermission())
        {
            addErrorMessage(getText("cpji.you.dont.have.create.issue.permission"));
            return ERROR;
        }

        checkIssueTypes(copyInfo.getIssueTypes());

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

    private void checkIssueTypes(final Collection<IssueTypeBean> values)
    {
        MutableIssue issue = getIssueObject();
        availableIssueTypes = Lists.newArrayList();
        for (IssueTypeBean value : values)
        {
            if (StringUtils.equalsIgnoreCase(value.getName(), issue.getIssueTypeObject().getName()))
            {
                availableIssueTypes.add(new Option(value.getName(), true));
            }
            else
            {
                availableIssueTypes.add(new Option(value.getName(), false));
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
		return (linksEnabled() || copyInfo.getIssueLinkingEnabled()) && !issueLinkTypeManager.getIssueLinkTypesByName("Copied").isEmpty();
	}

	public boolean isCopyCommentsSectionVisible() {
		return isIssueWithComments();
	}

	public String getCopyAttachmentsErrorMessage() {
		if (!copyInfo.getAttachmentsEnabled()) {
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

	public IssueTypeBean getRemoteIssueType() {
		final MutableIssue issue = getIssueObject();
		final IssueType issueType = issue.getIssueTypeObject();

		IssueTypeBean remoteIssueType = Iterables.getFirst(
				Iterables.filter(copyInfo.getIssueTypes(), IssueTypeBean.hasName(issueType.getName())), null);
		if (remoteIssueType == null) {
			remoteIssueType = Iterables.getFirst(copyInfo.getIssueTypes(), null);
		}
		return remoteIssueType;
	}

	public List<FieldLayoutItem> getFieldLayoutItems() {
		final MutableIssue issue = getIssueObject();
		final IssueType issueType = issue.getIssueTypeObject();

		final IssueTypeBean remoteIssueType = getRemoteIssueType();
		Preconditions.checkNotNull("Remote JIRA has not Issue Types", remoteIssueType);

		final Iterable<IssueFieldBean> fields = Iterables.filter(remoteIssueType.getRequiredFields(), Predicates.not(IssueFieldBean.hasId(
				RequiredFieldsAwareAction.UNMODIFIABLE_FIELDS)));

		return Lists.newArrayList(Iterables.transform(fields, new Function<IssueFieldBean, FieldLayoutItem>() {
			@Override
			public FieldLayoutItem apply(@Nullable IssueFieldBean input) {
				return fieldLayoutService.createDefaultFieldLayoutItem(input.getId(), true);
			}
		}));
	}

	public String getHtmlForField(FieldLayoutItem fieldLayoutItem)
	{
		final IssueTypeBean remoteIssueType = getRemoteIssueType();
		Preconditions.checkNotNull("Remote JIRA has not Issue Types", remoteIssueType);

		OrderableField orderableField = fieldLayoutItem.getOrderableField();
		Object defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(getSelectedDestinationProject().getProjectKey(),
				orderableField.getId(), remoteIssueType.getName());
		if (ActionContext.getParameters().get(orderableField.getId()) == null)
		{
			if (defaultFieldValue != null)
			{
				Map actionParams = new HashMap();
				actionParams.put(orderableField.getId(), defaultFieldValue);
				orderableField.populateFromParams(Maps.newHashMap(), actionParams);
			}
		}

		return orderableField.getEditHtml(fieldLayoutItem, this, this, getIssueObject(), getDisplayParameters());
	}

	@Override
	public Map getFieldValuesHolder() {
		return Maps.newHashMap();
	}

	@Override
	public IssueOperation getIssueOperation() {
		return IssueOperations.EDIT_ISSUE_OPERATION;
	}
}
