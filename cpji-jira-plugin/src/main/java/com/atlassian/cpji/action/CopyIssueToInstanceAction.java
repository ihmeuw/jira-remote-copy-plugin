package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.CopyIssueBeanFactory;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;

/**
 */
public class CopyIssueToInstanceAction extends AbstractCopyIssueAction {
    private String copiedIssueKey;
    private boolean copyAttachments;
    private boolean copyIssueLinks;
    private boolean copyComments;
    private String issueType;
    private String remoteIssueLink;

    private static final Logger log = Logger.getLogger(CopyIssueToInstanceAction.class);
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final CopyIssueBeanFactory copyIssueBeanFactory;
    private String linkToNewIssue;

    public CopyIssueToInstanceAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final IssueLinkManager issueLinkManager,
            final RemoteIssueLinkManager remoteIssueLinkManager,
            final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory, IssueLinkTypeManager issueLinkTypeManager,
            final WebResourceManager webResourceManager,
            final CopyIssueBeanFactory copyIssueBeanFactory) {
        super(subTaskManager, fieldLayoutManager, commentManager,
                copyIssuePermissionManager, applicationLinkService, jiraProxyFactory,
                webResourceManager);
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.copyIssueBeanFactory = copyIssueBeanFactory;
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS)) {
            return permissionCheck;
        }

        final SelectedProject linkToTargetEntity = getSelectedDestinationProject();
        final JiraProxy proxy = jiraProxyFactory.createJiraProxy(linkToTargetEntity.getJiraLocation());

        MutableIssue issueToCopy = getIssueObject();
        CopyIssueBean copyIssueBean = copyIssueBeanFactory.create(linkToTargetEntity.getProjectKey(), issueToCopy,
                issueType, copyComments);
        Either<NegativeResponseStatus, IssueCreationResultBean> result = proxy.copyIssue(copyIssueBean);
        IssueCreationResultBean copiedIssue = handleGenericResponseStatus(proxy, result, null);
        if (copiedIssue == null) {
            return getGenericResponseHandlerResult();
        }

        copiedIssueKey = copiedIssue.getIssueKey();
        final Collection<Attachment> attachments = issueToCopy.getAttachments();
        if (getCopyAttachments() && !attachments.isEmpty()) {
            for (final Attachment attachment : attachments) {
                File attachmentFile = AttachmentUtils.getAttachmentFile(attachment);
                Either<NegativeResponseStatus, SuccessfulResponse> addResult = proxy.addAttachment(copiedIssueKey, attachmentFile, attachment.getFilename(), attachment.getMimetype());
                if (addResult.isLeft()) {
                    NegativeResponseStatus responseStatus = (NegativeResponseStatus) addResult.left().get();
                    ErrorCollection ec = responseStatus.getErrorCollection();
                    if (ec != null) {
                        addErrorMessages(
                                Lists.newArrayList(
                                        Iterables.transform(ec.getErrorMessages(), new Function<String, String>() {
                                            @Override
                                            public String apply(@Nullable String input) {
                                                return attachment.getFilename() + ": " + input;
                                            }
                                        })));
                    }
                }
            }
        }

        if (getCopyIssueLinks() && issueLinkManager.isLinkingEnabled()) {
            copyLocalIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId());
            copyRemoteIssueLinks(proxy, issueToCopy, copiedIssue.getIssueKey());
            proxy.convertRemoteIssueLinksIntoLocal(copiedIssueKey);
        }

        if (StringUtils.isNotBlank(remoteIssueLink)) {
            final Collection<IssueLinkType> copiedTypeCollection = issueLinkTypeManager.getIssueLinkTypesByName("Copied");
            if (copiedTypeCollection.size() > 0) {
                final RemoteIssueLinkType remoteIssueLinkType = RemoteIssueLinkType.valueOf(remoteIssueLink);

                proxy.copyLocalIssueLink(issueToCopy, copiedIssue.getIssueKey(), copiedIssue.getIssueId(),
                        Iterables.get(copiedTypeCollection, 0),
                        remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.OUTWARD : JiraProxy.LinkCreationDirection.IGNORE,
                        remoteIssueLinkType.hasLocalIssueLinkToRemote() ? JiraProxy.LinkCreationDirection.INWARD : JiraProxy.LinkCreationDirection.IGNORE);
            }
        }

        linkToNewIssue = proxy.getIssueUrl(copiedIssue.getIssueKey());

        return SUCCESS;

    }


    private void copyLocalIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) throws ResponseException, CredentialsRequiredException {
        for (final IssueLink inwardLink : issueLinkManager.getInwardLinks(localIssue.getId())) {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId, type, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        }
        for (final IssueLink outwardLink : issueLinkManager.getOutwardLinks(localIssue.getId())) {

            final IssueLinkType type = outwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type, JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        }
    }

    private void copyRemoteIssueLinks(JiraProxy remoteJira, final Issue localIssue, final String copiedIssueKey) throws ResponseException, CredentialsRequiredException {
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue)) {
            remoteJira.copyRemoteIssueLink(remoteIssueLink, copiedIssueKey);
        }
    }

    public String getLinkToNewIssue() {
        return linkToNewIssue;
    }


    public String getCopiedIssueKey() {
        return copiedIssueKey;
    }

    public boolean getCopyAttachments() {
        return copyAttachments;
    }

    public void setCopyAttachments(final boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    public boolean getCopyComments() {
        return copyComments;
    }

    public void setCopyComments(boolean copyComments) {
        this.copyComments = copyComments;
    }

    public boolean getCopyIssueLinks() {
        return copyIssueLinks;
    }

    public void setCopyIssueLinks(final boolean copyIssueLinks) {
        this.copyIssueLinks = copyIssueLinks;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(final String issueType) {
        this.issueType = issueType;
    }

    public String getRemoteIssueLink() {
        return remoteIssueLink;
    }

    public void setRemoteIssueLink(final String remoteIssueLink) {
        this.remoteIssueLink = remoteIssueLink;
    }
}