package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.EntityLink;
import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.cpji.IssueAttachmentsClient;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.rest.ResponseHolder;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.rest.util.Holder;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.cpji.util.RestResponse;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.List;

/**
 */
public class CopyIssueToInstanceAction extends AbstractCopyIssueAction
{
    public static final int CONNECTION_TIMEOUTS = 100000;
    private String copiedIssueKey;
    private boolean copyAttachments;
    private String issueType;
    private String remoteIssueLink;

    private static final Logger log = Logger.getLogger(CopyIssueToInstanceAction.class);
    private final IssueLinkClient issueLinkClient;

    private class CreationResult
    {
        public boolean success;
        public List<String> errorMessages;

        private CreationResult(final boolean success)
        {
            this.success = success;
        }

        private CreationResult(final boolean success, final List<String> errorMessages)
        {
            this.success = success;
            this.errorMessages = errorMessages;
        }
    }

    public CopyIssueToInstanceAction
            (
                    final SubTaskManager subTaskManager,
                    final EntityLinkService entityLinkService,
                    final FieldLayoutManager fieldLayoutManager,
                    final CommentManager commentManager,
                    final FieldManager fieldManager,
                    final FieldMapperFactory fieldMapperFactory,
                    final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
                    final CopyIssuePermissionManager copyIssuePermissionManager,
                    final IssueLinkClient issueLinkClient)
    {
        super(subTaskManager, entityLinkService, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory, fieldLayoutItemsRetriever, copyIssuePermissionManager);
        this.issueLinkClient = issueLinkClient;
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
        EntityLink linkToTargetEntity = getSelectedEntityLink();
        ApplicationLink targetApplication = linkToTargetEntity.getApplicationLink();
        ApplicationLinkRequestFactory authenticatedRequestFactory = targetApplication.createAuthenticatedRequestFactory();

        ApplicationLinkRequest request = authenticatedRequestFactory.createRequest(Request.MethodType.PUT, REST_URL_COPY_ISSUE + COPY_ISSUE_RESOURCE_PATH + "/copy");
        request.setSoTimeout(CONNECTION_TIMEOUTS);
        request.setConnectionTimeout(CONNECTION_TIMEOUTS);
        MutableIssue issueToCopy = getIssueObject();

        CopyIssueBean copyIssueBean = createCopyIssueBean(linkToTargetEntity.getKey(), issueToCopy, issueType);
        request.setEntity(copyIssueBean);

        final Holder<IssueCreationResultBean> resultHolder = new Holder<IssueCreationResultBean>();

        CreationResult copied = request.execute(new ApplicationLinkResponseHandler<CreationResult>()
        {
            public CreationResult credentialsRequired(Response response) throws ResponseException
            {
                log.error("Failed to copy the issue. Response is '" + response.getResponseBodyAsString() + "'");
                return new CreationResult(false, Lists.newArrayList("Authentication failed!"));
            }

            public CreationResult handle(Response response) throws ResponseException
            {
                if (response.isSuccessful())
                {
                    resultHolder.value = response.getEntity(IssueCreationResultBean.class);
                    return new CreationResult(true);
                }
                ErrorBean errorBean = response.getEntity(ErrorBean.class);
                log.error("Failed to copy the issue. Error(s): '" + errorBean.getErrors() + "'");
                return new CreationResult(false, errorBean.getErrors());
            }
        });
        if (copied.success)
        {
            copiedIssueKey = resultHolder.value.getIssueKey();
            if (copyAttachments() && !issueToCopy.getAttachments().isEmpty())
            {
                IssueAttachmentsClient issueAttachmentsClient = new IssueAttachmentsClient(targetApplication);
                ResponseHolder copyAttachmentResponse = issueAttachmentsClient.addAttachment(copiedIssueKey, issueToCopy.getAttachments());
                if (!copyAttachmentResponse.isSuccessful())
                {
                    addErrorMessage("Failed to copy attachments. " + copyAttachmentResponse.getResponse());
                }
            }
            RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);
            if (remoteIssueLinkType.equals(RemoteIssueLinkType.INCOMING) || remoteIssueLinkType.equals(RemoteIssueLinkType.RECIPROCAL))
            {
                 RestResponse remoteIssueLinkResponse = issueLinkClient.createRemoteIssueLinkFromIssue(issueToCopy, targetApplication, copiedIssueKey, "copied from");
                 if (!remoteIssueLinkResponse.isSuccessful())
                 {
                     log.error("Failed to create remote issue link from '" + copiedIssueKey + "' to '" + issueToCopy.getKey() + "'");
                 }
            }
            if (remoteIssueLinkType.equals(RemoteIssueLinkType.OUTGOING) || remoteIssueLinkType.equals(RemoteIssueLinkType.RECIPROCAL))
            {
                issueLinkClient.createRemoteLinkToIssue(issueToCopy, targetApplication, resultHolder.value.getIssueKey(), resultHolder.value.getIssueId());
            }
            return SUCCESS;
        }
        addErrorMessages(copied.errorMessages);
        return ERROR;
    }

    public String getLinkToNewIssue()
    {
        URI displayUrl = getSelectedEntityLink().getApplicationLink().getDisplayUrl();
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