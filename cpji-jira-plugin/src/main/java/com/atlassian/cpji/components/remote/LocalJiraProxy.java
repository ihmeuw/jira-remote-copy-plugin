package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.components.exceptions.CopyIssueException;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.PluginVersion;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.model.SimplifiedIssueLinkType;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import io.atlassian.fugue.Either;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.AttachmentError;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import java.util.Collection;

/**
 * @since v3.0
 */
public class LocalJiraProxy implements JiraProxy {

	private final JiraLocation jiraLocation;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final CopyIssueService copyIssueService;
    private final AttachmentManager attachmentManager;
    private final IssueManager issueManager;
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final ProjectInfoService projectInfoService;
    private final JiraBaseUrls jiraBaseUrls;


    public LocalJiraProxy(final PermissionManager permissionManager, final JiraAuthenticationContext jiraAuthenticationContext, CopyIssueService copyIssueService, AttachmentManager attachmentManager, IssueManager issueManager, IssueLinkManager issueLinkManager, RemoteIssueLinkManager remoteIssueLinkManager, ProjectInfoService projectInfoService, JiraBaseUrls jiraBaseUrls, ApplicationProperties applicationProperties) {
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.copyIssueService = copyIssueService;
        this.attachmentManager = attachmentManager;
        this.issueManager = issueManager;
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.projectInfoService = projectInfoService;
        this.jiraBaseUrls = jiraBaseUrls;
        jiraLocation = new JiraLocation(JiraLocation.LOCAL.getId(), applicationProperties.getString(APKeys.JIRA_TITLE));

    }

    @Override
    public JiraLocation getJiraLocation() {
        return jiraLocation;
    }

    @Override
    public Either<NegativeResponseStatus, Projects> getProjects() {
        Collection<Project> projects = permissionManager.getProjects(ProjectPermissions.CREATE_ISSUES, jiraAuthenticationContext.getUser());

        Iterable<BasicProject> basicProjects = Iterables.transform(projects, input -> {
            Preconditions.checkNotNull(input);
            return new BasicProject(null, input.getKey(), input.getId(), input.getName());
        });

        return Either.right(new Projects(jiraLocation, basicProjects));

    }

    @Override
    public Either<NegativeResponseStatus, PluginVersion> isPluginInstalled() {
        return Either.right(new PluginVersion(jiraLocation, PluginInfoResource.PLUGIN_VERSION));
    }

    @Override
    public String generateAuthenticationUrl(String issueId) {
        return null;
    }

    @Override
    public Either<NegativeResponseStatus, CopyInformationBean> getCopyInformation(String projectKey) {
        try {
            return Either.right(projectInfoService.getIssueTypeInformation(projectKey));
        } catch (ProjectNotFoundException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getErrorCollection()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean) {
        try {
            if(projectInfoService.isIssueTypeASubtask(copyIssueBean.getTargetIssueType(), copyIssueBean.getTargetProjectKey())){
                MutableIssue sourceIssue = issueManager.getIssueObject(copyIssueBean.getOriginalKey());
                if(sourceIssue.getProjectObject().getKey().equals(copyIssueBean.getTargetProjectKey())){
                    copyIssueBean.setTargetParentId(sourceIssue.getParentId());
                }

            }
            return Either.right(copyIssueService.copyIssue(copyIssueBean));
        } catch (CopyIssueException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getErrorCollection()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> addAttachment(String issueKey, Attachment attachment) {
        Either<AttachmentError, Attachment> result = attachmentManager.copyAttachment(attachment, jiraAuthenticationContext.getUser(), issueKey);
        if (result.isLeft()) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, result.left().get().getLogMessage()));
        }
        return Either.right(SuccessfulResponse.build(jiraLocation));
    }


    @Override
    public Either<NegativeResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean) {
        try {
            return Either.right(copyIssueService.checkFieldPermissions(copyIssueBean));
        } catch (ProjectNotFoundException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getErrorCollection()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
        return copyLocalIssueLink(localIssue, remoteIssueKey, remoteIssueId, new SimplifiedIssueLinkType(issueLinkType), localDirection, remoteDirection);
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, SimplifiedIssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
        try {
            //In contrary to remote links, local are always created in both directions so is is not possible to create only one-way local link.
            //So here we care only local direction (ie creating link from source issue)
            if (localDirection == LinkCreationDirection.OUTWARD) {
                issueLinkManager.createIssueLink(localIssue.getId(), remoteIssueId, issueLinkType.getId(), null, jiraAuthenticationContext.getLoggedInUser());
            } else if (localDirection == LinkCreationDirection.INWARD) {
                issueLinkManager.createIssueLink(remoteIssueId, localIssue.getId(), issueLinkType.getId(), null, jiraAuthenticationContext.getLoggedInUser());
            }
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (CreateException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey) {
        Issue issue = issueManager.getIssueObject(remoteIssueKey);
        try {
            RemoteIssueLinkBuilder builder = new RemoteIssueLinkBuilder(remoteIssueLink).issueId(issue.getId()).id(null);
            remoteIssueLinkManager.createRemoteIssueLink(builder.build(), jiraAuthenticationContext.getUser());
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (CreateException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {
        //all local links are also local
        //all remote links are still remote
        //so it is dry run
        return SuccessfulResponse.buildEither(jiraLocation);
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> clearChangeHistory(String issueKey) {
        try{
            copyIssueService.clearChangeHistory(issueKey);
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (CopyIssueException e){
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getErrorCollection()));
        }
    }

    @Override
    public String getIssueUrl(String issueKey) {
        String baseUrl = jiraBaseUrls.baseUrl();
        return baseUrl + "/browse/" + issueKey;
    }

}
