package com.atlassian.cpji.components;

import com.atlassian.cpji.action.admin.RequiredFieldsAwareAction;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueFieldBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @since v3.0
 */
public class ProjectInfoService {

    private final ProjectService projectService;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final ApplicationProperties applicationProperties;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final PermissionManager permissionManager;
    private final BuildUtilsInfo buildUtilsInfo;
	private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;

    public ProjectInfoService(ProjectService projectService, IssueTypeSchemeManager issueTypeSchemeManager,
			ApplicationProperties applicationProperties, JiraAuthenticationContext jiraAuthenticationContext,
			PermissionManager permissionManager, BuildUtilsInfo buildUtilsInfo, FieldLayoutItemsRetriever fieldLayoutItemsRetriever) {
        this.projectService = projectService;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.applicationProperties = applicationProperties;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
        this.buildUtilsInfo = buildUtilsInfo;
		this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
	}

    public CopyInformationBean getIssueTypeInformation(String projectKey) throws ProjectNotFoundException {
        final User user = jiraAuthenticationContext.getLoggedInUser();
        ProjectService.GetProjectResult result = projectService.getProjectByKey(user, projectKey);
        Project project;
        if (result.isValid()) {
            project = result.getProject();
        } else {
            throw new ProjectNotFoundException(result.getErrorCollection());
        }

        final UserBean userBean = new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
        final boolean hasCreateIssuePermission = permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, user);
        final boolean hasCreateAttachmentPermission = permissionManager.hasPermission(Permissions.CREATE_ATTACHMENT, project, user);
		final boolean hasCreateCommentPermission = permissionManager.hasPermission(Permissions.COMMENT_ISSUE, project, user);
		final boolean hasCreateLinksPermission = permissionManager.hasPermission(Permissions.LINK_ISSUE, project, user);
        final long maxAttachmentSize = Long.parseLong(applicationProperties.getString(APKeys.JIRA_ATTACHMENT_SIZE));


        CopyInformationBean copyInformationBean = new CopyInformationBean(
                hasCreateIssuePermission?getMasterIssueTypes(project):null,
                applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS),
                applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING),
                userBean,
                hasCreateIssuePermission, hasCreateAttachmentPermission, hasCreateCommentPermission, hasCreateLinksPermission,
                buildUtilsInfo.getVersion(),
                maxAttachmentSize);
        return copyInformationBean;

    }

	protected Collection<IssueTypeBean> getMasterIssueTypes(final Project project) {
		return Collections2.transform(
                Collections2.filter(issueTypeSchemeManager.getIssueTypesForProject(project), new Predicate<IssueType>() {
                    @Override
                    public boolean apply(@Nullable IssueType input) {
                        return input != null && !input.isSubTask();
                    }
                }), new Function<IssueType, IssueTypeBean>() {
			@Override
			public IssueTypeBean apply(IssueType issueType) {
				return new IssueTypeBean(issueType.getName(), getRequiredFields(project, issueType));
			}
		});
	}

	protected List<IssueFieldBean> getRequiredFields(Project project, final IssueType issueType)
	{
		Iterable<FieldLayoutItem> filter = Iterables
				.filter(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType),
						new Predicate<FieldLayoutItem>() {
							public boolean apply(final FieldLayoutItem input) {
								return !RequiredFieldsAwareAction.UNMODIFIABLE_FIELDS.contains(input.getOrderableField().getId()) && input
										.isRequired();
							}
						});
		return Lists.newArrayList(Iterables.transform(filter, new Function<FieldLayoutItem, IssueFieldBean>() {
			@Override
			public IssueFieldBean apply(FieldLayoutItem input) {
				return new IssueFieldBean(input.getOrderableField().getName(), input.getOrderableField().getId());
			}
		}));
	}
}
