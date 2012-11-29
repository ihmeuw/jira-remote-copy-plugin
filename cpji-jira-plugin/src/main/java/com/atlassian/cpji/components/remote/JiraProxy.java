package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.attachment.Attachment;
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
    public ErrorCollection copyAttachments(final String issueKey, Collection<Attachment> attachments);
    public void copyIssueLinks(Long localIssueId, Long remoteIssueId, String remoteIssueKey);
}
