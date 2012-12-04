package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.components.SuccessfulResponse;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;

import java.io.File;

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
//    public ErrorCollection copyAttachments(String issueKey, Collection<Attachment> attachments);
    public Either<ResponseStatus, SuccessfulResponse> addAttachment(String issueKey, File attachmentFile, String filename, String contentType);
    public Either<ResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean);
    public void copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection);
//    public void copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, String localRelationship, String remoteRelationship);
    public void copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey);
    public void convertRemoteIssueLinksIntoLocal(String remoteIssueKey);
    public String getIssueUrl(String issueKey);

    public static enum LinkCreationDirection{
        OUTWARD, INWARD, IGNORE;
        public String getNameFromIssueLinkType(IssueLinkType linkType){
            switch(this){
                case OUTWARD:
                    return linkType.getOutward();
                case INWARD:
                    return linkType.getInward();
                default:
                    return null;
            }
        }
    }

}
