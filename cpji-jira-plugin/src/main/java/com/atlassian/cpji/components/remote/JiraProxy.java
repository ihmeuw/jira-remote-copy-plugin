package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.model.*;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import io.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;

import java.io.File;

/**
 * @since v3.0
 */
public interface JiraProxy {
    JiraLocation getJiraLocation();

    Either<NegativeResponseStatus, Projects> getProjects();

    Either<NegativeResponseStatus, PluginVersion> isPluginInstalled();

    Either<NegativeResponseStatus, CopyInformationBean> getCopyInformation(String projectKey);

    Either<NegativeResponseStatus, IssueCreationResultBean> copyIssue(CopyIssueBean copyIssueBean);

    Either<NegativeResponseStatus, SuccessfulResponse> addAttachment(String issueKey, Attachment originalAttachment);

    Either<NegativeResponseStatus, FieldPermissionsBean> checkPermissions(CopyIssueBean copyIssueBean);

    Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection);

    Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, SimplifiedIssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection);

    Either<NegativeResponseStatus, SuccessfulResponse> copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String remoteIssueKey);

    Either<NegativeResponseStatus, SuccessfulResponse> convertRemoteIssueLinksIntoLocal(String remoteIssueKey);

    Either<NegativeResponseStatus, SuccessfulResponse> clearChangeHistory(String issueKey);

    String generateAuthenticationUrl(String issueId);

    String getIssueUrl(String issueKey);

    enum LinkCreationDirection {
        OUTWARD, INWARD, IGNORE;

        public String getNameFromIssueLinkType(SimplifiedIssueLinkType linkType) {
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
