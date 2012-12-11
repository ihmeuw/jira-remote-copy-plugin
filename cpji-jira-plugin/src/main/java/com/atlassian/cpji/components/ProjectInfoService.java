package com.atlassian.cpji.components;

import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.Collection;

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

    public ProjectInfoService(ProjectService projectService, IssueTypeSchemeManager issueTypeSchemeManager,
			ApplicationProperties applicationProperties, JiraAuthenticationContext jiraAuthenticationContext,
			PermissionManager permissionManager, BuildUtilsInfo buildUtilsInfo) {
        this.projectService = projectService;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.applicationProperties = applicationProperties;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
        this.buildUtilsInfo = buildUtilsInfo;
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

        if (hasCreateIssuePermission) {
            CopyInformationBean copyInformationBean = new CopyInformationBean(getIssueTypes(project),
					applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS),
					applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING),
					userBean,
					hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return copyInformationBean;
        } else {
            CopyInformationBean copyInformationBean = new CopyInformationBean(null,
					applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS),
					applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING), userBean,
					hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return copyInformationBean;
        }
    }

	protected Collection<IssueTypeBean> getIssueTypes(final Project project) {
		return Collections2.transform(issueTypeSchemeManager.getIssueTypesForProject(project), new Function<IssueType, IssueTypeBean>() {
			@Override
			public IssueTypeBean apply(IssueType issueType) {
				return new IssueTypeBean(issueType.getName(), Lists.<String>newArrayList());
			}
		});
	}
}
