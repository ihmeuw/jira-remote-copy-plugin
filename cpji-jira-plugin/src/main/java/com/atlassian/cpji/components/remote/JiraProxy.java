package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.model.SuccessfulResponse;
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
 * @since v3.0
 */
public interface JiraProxy {
    public JiraLocation getJiraLocation();

    public Either<NegativeResponseStatus, Projects> getProjects();

    public Either<NegativeResponseStatus, SuccessfulResponse> isPluginInstalled();



    public Either<NegativeResponseStatus, CopyInformationBean> getCopyInformation(String projectKey);

    public Either<NegativeResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean);

    public Either<NegativeResponseStatus, SuccessfulResponse> addAttachment(String issueKey, File attachmentFile, String filename, String contentType);

    public Either<NegativeResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean);

    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection);

    public Either<NegativeResponseStatus, SuccessfulResponse> copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey);

    public Either<NegativeResponseStatus, SuccessfulResponse> convertRemoteIssueLinksIntoLocal(String remoteIssueKey);

    public String generateAuthenticationUrl(String issueId);

    public String getIssueUrl(String issueKey);

    public static enum LinkCreationDirection {
        OUTWARD, INWARD, IGNORE;

        public String getNameFromIssueLinkType(IssueLinkType linkType) {
            switch (this) {
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
