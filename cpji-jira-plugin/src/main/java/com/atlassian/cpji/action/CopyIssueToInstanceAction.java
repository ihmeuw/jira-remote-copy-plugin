package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collection;

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
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private String linkToNewIssue;

    public CopyIssueToInstanceAction
            (
                    final SubTaskManager subTaskManager,
                    final FieldLayoutManager fieldLayoutManager,
                    final CommentManager commentManager,
                    final FieldManager fieldManager,
                    final FieldMapperFactory fieldMapperFactory,
                    final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
                    final CopyIssuePermissionManager copyIssuePermissionManager,
                    final UserMappingManager userMappingManager,
                    final IssueLinkManager issueLinkManager,
                    final RemoteIssueLinkManager remoteIssueLinkManager,
                    final ApplicationLinkService applicationLinkService,
                    final JiraProxyFactory jiraProxyFactory, IssueLinkTypeManager issueLinkTypeManager,
					final WebResourceManager webResourceManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory,
				fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory,
				webResourceManager);
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
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
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(linkToTargetEntity.getJiraLocation());

        MutableIssue issueToCopy = getIssueObject();
        CopyIssueBean copyIssueBean = createCopyIssueBean(linkToTargetEntity.getProjectKey(), issueToCopy, issueType);
        Either<NegativeResponseStatus, IssueCreationResultBean> result = proxy.copyIssue(copyIssueBean);
        IssueCreationResultBean copiedIssue = handleGenericResponseStatus(proxy, result, null);
        if(copiedIssue == null){
            return getGenericResponseHandlerResult();
        }

        copiedIssueKey = copiedIssue.getIssueKey();

        if (copyAttachments() && !issueToCopy.getAttachments().isEmpty())
        {
            for(Attachment attachment : issueToCopy.getAttachments()){
                File attachmentFile = AttachmentUtils.getAttachmentFile(attachment);
                Either<NegativeResponseStatus, SuccessfulResponse> addResult = proxy.addAttachment(copiedIssueKey, attachmentFile, attachment.getFilename(), attachment.getMimetype());
                if(addResult.isLeft()){
                    NegativeResponseStatus responseStatus = (NegativeResponseStatus) addResult.left().get();
                    ErrorCollection ec = responseStatus.getErrorCollection();
                    if(ec != null){
                        addErrorCollection(ec);
                    }
                }
            }
        }

        if (copyIssueLinks() && issueLinkManager.isLinkingEnabled())
        {
            copyLocalIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId());
            copyRemoteIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey());
            proxy.convertRemoteIssueLinksIntoLocal(copiedIssueKey);
        }

        RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);


        Collection<IssueLinkType> copiedTypeCollection = issueLinkTypeManager.getIssueLinkTypesByName("Copied");
        if(copiedTypeCollection.size() > 0){

            proxy.copyLocalIssueLink(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId(),
                     Iterables.get(copiedTypeCollection, 0),
                    remoteIssueLinkType.hasLocalIssueLinkToRemote()?JiraProxy.LinkCreationDirection.OUTWARD:JiraProxy.LinkCreationDirection.IGNORE,
                    remoteIssueLinkType.hasLocalIssueLinkToRemote()?JiraProxy.LinkCreationDirection.INWARD:JiraProxy.LinkCreationDirection.IGNORE);
        }

        linkToNewIssue = proxy.getIssueUrl(copiedIssue.getIssueKey());

        return SUCCESS;

    }


    private void copyLocalIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) throws ResponseException, CredentialsRequiredException
    {
        for (final IssueLink inwardLink : issueLinkManager.getInwardLinks(localIssue.getId()))
        {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId,  type, JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        }
        for (final IssueLink outwardLink : issueLinkManager.getOutwardLinks(localIssue.getId()))
        {

            final IssueLinkType type = outwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        }
    }

    private void copyRemoteIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey) throws ResponseException, CredentialsRequiredException
    {
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue))
        {
            remoteJira.copyRemoteIssueLink(remoteIssueLink, copiedIssueKey);
        }
    }

    public String getLinkToNewIssue(){
        return linkToNewIssue;
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