package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.ErrorCollection;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 *
 * @since v3.0
 */
public class LocalJiraProxy implements JiraProxy
{

    public static final JiraLocation LOCAL_JIRA_LOCATION = new JiraLocation("LOCAL", "");
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public LocalJiraProxy(final PermissionManager permissionManager, final JiraAuthenticationContext jiraAuthenticationContext) {
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    @Override
    public JiraLocation getJiraLocation() {
        return LOCAL_JIRA_LOCATION;
    }

    @Override
    public Either<ResponseStatus, Projects> getProjects()
    {
        Collection<Project> projects = permissionManager.getProjectObjects(Permissions.CREATE_ISSUE, jiraAuthenticationContext.getLoggedInUser());

        Iterable<BasicProject> basicProjects = Iterables.transform(projects, new Function<Project, BasicProject>()
        {
            @Override
            public BasicProject apply(@Nullable final Project input)
            {
                return new BasicProject(null, input.getKey(), input.getName());
            }
        });


        return Either.right(new Projects(LOCAL_JIRA_LOCATION, basicProjects));

    }

    @Override
    public ResponseStatus isPluginInstalled() {
        return ResponseStatus.ok(LOCAL_JIRA_LOCATION);
    }

    @Override
    public String generateAuthenticationUrl(String issueId) {
        throw new UnsupportedOperationException("Cannot generate authentication URL for local project");
    }

    @Override
    public Either<ResponseStatus, CopyInformationBean> getCopyInformation(String projectKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Either<ResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ErrorCollection copyAttachments(String issueKey, Collection<Attachment> attachments) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Either<ResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, String localRelationship, String remoteRelationship) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {
        throw new UnsupportedOperationException("Not implemented");
    }


}
