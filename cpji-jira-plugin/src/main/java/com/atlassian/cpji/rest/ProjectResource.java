package com.atlassian.cpji.rest;

import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.fields.rest.json.beans.ProjectJsonBean;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.BuildUtilsInfo;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("project")
@Consumes ({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class ProjectResource {

    private final ProjectService projectService;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final ApplicationProperties applicationProperties;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final PermissionManager permissionManager;
    private final BuildUtilsInfo buildUtilsInfo;
    private final JiraBaseUrls baseUrls;

    public ProjectResource(final ProjectService projectService, final IssueTypeSchemeManager issueTypeSchemeManager, final ApplicationProperties applicationProperties,
			final JiraAuthenticationContext jiraAuthenticationContext, final PermissionManager permissionManager,
			final BuildUtilsInfo buildUtilsInfo, final JiraBaseUrls baseUrls) {
        this.projectService = projectService;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.applicationProperties = applicationProperties;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
        this.buildUtilsInfo = buildUtilsInfo;
        this.baseUrls = baseUrls;
    }

    @GET
    @Path ("issueTypeInformation/{project}")
    public Response getIssueTypeInformation(@PathParam("project") String projectKey)
    {
        User user = callingUser();
        ProjectService.GetProjectResult result = projectService.getProjectByKey(user, projectKey);
        Project project;
        if (result.isValid())
        {
            project = result.getProject();
        }
        else
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(result.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
        UserBean userBean = new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
        boolean hasCreateIssuePermission = permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, user);
        boolean hasCreateAttachmentPermission = permissionManager.hasPermission(Permissions.CREATE_ATTACHMENT, project, user);

        if (hasCreateIssuePermission)
        {
            Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
            List<String> issueTypes = new ArrayList<String>();
            for (IssueType issueType : issueTypesForProject)
            {
                issueTypes.add(issueType.getName());
            }
            IssueTypeBean issueTypesBean = new IssueTypeBean(issueTypes);
            boolean attachmentsDisabled = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
            CopyInformationBean copyInformationBean = new CopyInformationBean(issueTypesBean, attachmentsDisabled, userBean, hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return Response.ok(copyInformationBean).cacheControl(RESTException.never()).build();
        }
        else
        {
            CopyInformationBean copyInformationBean = new CopyInformationBean(null, true, userBean, hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return Response.ok(copyInformationBean).cacheControl(RESTException.never()).build();
        }
    }

    @GET
    public Response getProjectsWithCreateIssuePermission(){
        Collection<Project> projects = permissionManager.getProjectObjects(Permissions.CREATE_ISSUE, callingUser());
        return Response.ok(ProjectJsonBean.shortBeans(projects, baseUrls)).build();
    }

    private User callingUser()
    {
        return jiraAuthenticationContext.getLoggedInUser();
    }
}
