package com.atlassian.cpji.rest;

import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.fields.rest.json.beans.ProjectJsonBean;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("project")
@Consumes ({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class ProjectResource {

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final PermissionManager permissionManager;
    private final JiraBaseUrls baseUrls;
    private final ProjectInfoService projectInfoService;

    public ProjectResource(final JiraAuthenticationContext jiraAuthenticationContext, final PermissionManager permissionManager,
			final JiraBaseUrls baseUrls, ProjectInfoService projectInfoService) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
        this.baseUrls = baseUrls;
        this.projectInfoService = projectInfoService;
    }

    @GET
    @Path ("issueTypeInformation/{project}")
    public Response getIssueTypeInformation(@PathParam("project") String projectKey)
    {
        try {
            CopyInformationBean result = projectInfoService.getIssueTypeInformation(projectKey);
            return Response.ok(result).cacheControl(RESTException.never()).build();
        } catch (ProjectNotFoundException e) {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
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
