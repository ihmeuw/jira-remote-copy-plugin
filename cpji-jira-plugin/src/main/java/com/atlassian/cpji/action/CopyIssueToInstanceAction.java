package com.atlassian.cpji.action;

import com.atlassian.applinks.api.*;
import com.atlassian.cpji.IssueAttachmentsClient;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.ResponseHolder;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CreationResult;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.rest.util.Holder;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.cpji.util.RestResponse;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.net.URI;

/**
 */
public class CopyIssueToInstanceAction extends AbstractCopyIssueAction
{
    public static final int CONNECTION_TIMEOUTS = 100000;
    private String copiedIssueKey;
    private boolean copyAttachments;
    private boolean copyIssueLinks;
    private String issueType;
    private String remoteIssueLink;

    private static final Logger log = Logger.getLogger(CopyIssueToInstanceAction.class);
    private final IssueLinkClient issueLinkClient;
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;

    public CopyIssueToInstanceAction
            (
                    final SubTaskManager subTaskManager,
                    final FieldLayoutManager fieldLayoutManager,
                    final CommentManager commentManager,
                    final FieldManager fieldManager,
                    final FieldMapperFactory fieldMapperFactory,
                    final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
                    final CopyIssuePermissionManager copyIssuePermissionManager,
                    final IssueLinkClient issueLinkClient,
                    final UserMappingManager userMappingManager,
                    final IssueLinkManager issueLinkManager,
                    final RemoteIssueLinkManager remoteIssueLinkManager,
					final ApplicationLinkService applicationLinkService,
                    final JiraProxyFactory jiraProxyFactory)
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory,
				fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory);
        this.issueLinkClient = issueLinkClient;
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        
        final SelectedProject linkToTargetEntity = getSelectedDestinationProject();
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(linkToTargetEntity.getApplicationId());

        MutableIssue issueToCopy = getIssueObject();
        CopyIssueBean copyIssueBean = createCopyIssueBean(linkToTargetEntity.getProjectKey(), issueToCopy, issueType);
        Either<ResponseStatus, IssueCreationResultBean> result = proxy.copyIssue(copyIssueBean);

        if(result.isLeft()){
            ResponseStatus status = (ResponseStatus) result.left().get();

            if(ResponseStatus.Status.AUTHENTICATION_FAILED.equals(status.getResult())){
                log.error("Authentication failed.");
                addErrorMessage("Authentication failed. If using Trusted Apps, do you have a user with the same user name in the remote JIRA instance?");
            } else if(ResponseStatus.Status.AUTHORIZATION_REQUIRED.equals(status.getResult())){
                log.error("OAuth token invalid.");
            } else if(ResponseStatus.Status.COMMUNICATION_FAILED.equals(status.getResult())){
                log.error("Failed to copy the issue.");
                addErrorMessage("Failed to copy the issue.");
            }

            return ERROR;
        }

        IssueCreationResultBean copiedIssue = (IssueCreationResultBean) result.right().get();
        copiedIssueKey = copiedIssue.getIssueKey();
        if (copyAttachments() && !issueToCopy.getAttachments().isEmpty())
        {
            ErrorCollection attachmentsResult = proxy.copyAttachments(copiedIssueKey, issueToCopy.getAttachments());
            if(attachmentsResult.hasAnyErrors()){
                addErrorCollection(attachmentsResult);
            }
        }

        if (copyIssueLinks() && issueLinkManager.isLinkingEnabled())
        {
            proxy.copyIssueLinks(issueToCopy.getId(), copiedIssue.getIssueId(), copiedIssueKey);

        }

        RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);
        if (remoteIssueLinkType.equals(RemoteIssueLinkType.INCOMING) || remoteIssueLinkType.equals(RemoteIssueLinkType.RECIPROCAL))
        {
             RestResponse remoteIssueLinkResponse = issueLinkClient.createLinkFromRemoteIssue(issueToCopy, appLink, copiedIssueKey, "copied from");
             if (!remoteIssueLinkResponse.isSuccessful())
             {
                 log.error("Failed to create remote issue link from '" + copiedIssueKey + "' to '" + issueToCopy.getKey() + "'");
             }
        }
        if (remoteIssueLinkType.equals(RemoteIssueLinkType.OUTGOING) || remoteIssueLinkType.equals(RemoteIssueLinkType.RECIPROCAL))
        {
            issueLinkClient.createLinkToRemoteIssue(issueToCopy, appLink, resultHolder.value.getIssueKey(), resultHolder.value.getIssueId(), "copied to");
        }
        return SUCCESS;

    }



    public String getLinkToNewIssue() throws TypeNotInstalledException {
        URI displayUrl = applicationLinkService.getApplicationLink(getSelectedDestinationProject().getApplicationId()).getDisplayUrl();
        return displayUrl + "/browse/" + copiedIssueKey;
    }

    public String getCopiedIssueKey()
    {
        return copiedIssueKey;
    }

    public boolean copyAttachments()
    {
        return copyAttachments;
    }

    public void setCopyAttachments(final boolean copyAttachments)
    {
        this.copyAttachments = copyAttachments;
    }

    public boolean copyIssueLinks()
    {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(final boolean copyIssueLinks)
    {
        this.copyIssueLinks = copyIssueLinks;
    }

    public String getIssueType()
    {
        return issueType;
    }

    public void setIssueType(final String issueType)
    {
        this.issueType = issueType;
    }

    public String getRemoteIssueLink()
    {
        return remoteIssueLink;
    }

    public void setRemoteIssueLink(final String remoteIssueLink)
    {
        this.remoteIssueLink = remoteIssueLink;
    }
}