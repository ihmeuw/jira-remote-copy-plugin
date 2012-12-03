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
import com.atlassian.jira.util.ErrorCollection;

import java.util.Collection;

/**
 *
 * @since v3.0
 */
public interface JiraProxy
{
    public JiraLocation getJiraLocation();
    public Either<ResponseStatus, Projects> getProjects();
    public ResponseStatus isPluginInstalled();
    public String generateAuthenticationUrl(String issueId);
    public Either<ResponseStatus, CopyInformationBean> getCopyInformation(String projectKey);
    public Either<ResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean);
    public ErrorCollection copyAttachments(String issueKey, Collection<Attachment> attachments);
    public Either<ResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean);
    public void copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, String localRelationship, String remoteRelationship);
    public void copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey);
    public void convertRemoteIssueLinksIntoLocal(String remoteIssueKey);

}
