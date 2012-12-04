package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.*;
import com.atlassian.cpji.components.exceptions.CopyIssueException;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.util.AttachmentException;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.io.File;
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
    private final CopyIssueService copyIssueService;
    private final AttachmentManager attachmentManager;
    private final IssueManager issueManager;
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final ProjectInfoService projectInfoService;
    private final JiraBaseUrls jiraBaseUrls;

    public LocalJiraProxy(final PermissionManager permissionManager, final JiraAuthenticationContext jiraAuthenticationContext, CopyIssueService copyIssueService, AttachmentManager attachmentManager, IssueManager issueManager, IssueLinkManager issueLinkManager, RemoteIssueLinkManager remoteIssueLinkManager, ProjectInfoService projectInfoService, JiraBaseUrls jiraBaseUrls) {
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.copyIssueService = copyIssueService;
        this.attachmentManager = attachmentManager;
        this.issueManager = issueManager;
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.projectInfoService = projectInfoService;
        this.jiraBaseUrls = jiraBaseUrls;
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
        try {
            return Either.right(projectInfoService.getIssueTypeInformation(projectKey));
        } catch (ProjectNotFoundException e) {
            return Either.left(ResponseStatus.errorOccured(LOCAL_JIRA_LOCATION, e.getErrorCollection()));
        }
    }

    @Override
    public Either<ResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean) {
        try {
            return Either.right(copyIssueService.copyIssue(copyIssueBean));
        } catch (CopyIssueException e) {
            return Either.left(ResponseStatus.errorOccured(LOCAL_JIRA_LOCATION, e.getErrorCollection()));
        }
    }

    @Override
    public Either<ResponseStatus, SuccessfulResponse> addAttachment(String issueKey, File attachmentFile, String filename, String contentType) {
        Issue issue = issueManager.getIssueObject(issueKey);
        try {
            attachmentManager.createAttachment(attachmentFile, filename, contentType, jiraAuthenticationContext.getLoggedInUser(), issue);
            return Either.right(new SuccessfulResponse(LOCAL_JIRA_LOCATION));
        } catch (AttachmentException e) {
            return Either.left(ResponseStatus.errorOccured(LOCAL_JIRA_LOCATION, e.getMessage()));
        }
    }


    @Override
    public Either<ResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean) {
        try {
            return Either.right(copyIssueService.checkFieldPermissions(copyIssueBean));
        } catch (ProjectNotFoundException e) {
            return Either.left(ResponseStatus.errorOccured(LOCAL_JIRA_LOCATION, e.getMessage()));
        }
    }

    @Override
    public void copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
        try {

            if(localDirection == LinkCreationDirection.OUTWARD){
                issueLinkManager.createIssueLink(localIssue.getId(), remoteIssueId, issueLinkType.getId(), null, jiraAuthenticationContext.getLoggedInUser());
            } else if(localDirection == LinkCreationDirection.INWARD) {
                issueLinkManager.createIssueLink(remoteIssueId, localIssue.getId(), issueLinkType.getId(), null, jiraAuthenticationContext.getLoggedInUser());
            }

        } catch (CreateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey) {
        Issue issue = issueManager.getIssueObject(remoteIssueKey);
        try {
            RemoteIssueLinkBuilder builder = new RemoteIssueLinkBuilder(remoteIssueLink).issueId(issue.getId()).id(null);
            remoteIssueLinkManager.createRemoteIssueLink(builder.build(), jiraAuthenticationContext.getLoggedInUser());
        } catch (CreateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {
        //all local links are also local
        //all remote links are still remote
        //so it is dry run
    }

    @Override
    public String getIssueUrl(String issueKey) {
        String baseUrl = jiraBaseUrls.baseUrl();
        return baseUrl + "/browse/" + issueKey;
    }


}
