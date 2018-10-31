package com.atlassian.cpji.util;

import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SimplifiedIssueLinkType;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import io.atlassian.fugue.Either;

/**
 * Orchestrates copying local and remote links
 *
 * @since v3.0
 */
public class IssueLinkCopier {

    private final IssueLinkManager issueLinkManager;
    private final JiraProxy remoteJira;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final SimplifiedIssueLinkType clonerIssueLinkType;


    public static final Predicate<IssueLink> isNotSubtaskIssueLink = new Predicate<IssueLink>() {
        @Override
        public boolean apply(IssueLink input) {
            Preconditions.checkNotNull(input);
            return !input.getIssueLinkType().isSubTaskLinkType();
        }
    };

    public static Predicate<IssueLink> isNotSpecifiedLinkType(final SimplifiedIssueLinkType issueLinkType) {
        Preconditions.checkNotNull(issueLinkType);
        return new Predicate<IssueLink>() {
            @Override
            public boolean apply(IssueLink input) {
                Preconditions.checkNotNull(input);
                return !(issueLinkType.getInward().equals(input.getIssueLinkType().getInward())
                        && issueLinkType.getOutward().equals(input.getIssueLinkType().getOutward()));
            }
        };
    }

    public static Predicate<RemoteIssueLink> isNotSpecifiedRemoteLinkType(final SimplifiedIssueLinkType issueLinkType) {
        Preconditions.checkNotNull(issueLinkType);
        return new Predicate<RemoteIssueLink>() {
            @Override
            public boolean apply(RemoteIssueLink input) {
                Preconditions.checkNotNull(input);
                return !(input.getRelationship().equals(issueLinkType.getInward())
                        || input.getRelationship().equals(issueLinkType.getOutward()));
            }
        };
    }


    public IssueLinkCopier(IssueLinkManager issueLinkManager, RemoteIssueLinkManager remoteIssueLinkManager, JiraProxy remoteJira, final SimplifiedIssueLinkType clonerIssueLinkType) {
        this.issueLinkManager = issueLinkManager;
        this.remoteJira = remoteJira;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.clonerIssueLinkType = clonerIssueLinkType;
    }

    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalAndRemoteLinks(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) {
        copyLocalIssueLinks(localIssue, copiedIssueKey, copiedIssueId);
        copyRemoteIssueLinks(localIssue, copiedIssueKey);
        return remoteJira.convertRemoteIssueLinksIntoLocal(copiedIssueKey);
    }


    public void copyRemoteIssueLinks(final Issue localIssue, final String copiedIssueKey) {
        Iterable<RemoteIssueLink> remoteIssueLinksForIssue = Iterables.filter(remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue), isNotSpecifiedRemoteLinkType(clonerIssueLinkType));
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinksForIssue) {
            remoteJira.copyRemoteIssueLink(remoteIssueLink, copiedIssueKey);
        }
    }


    public void copyLocalIssueLinks(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) {

        //we throw out all subtask and cloner links before copying
        final Predicate<IssueLink> isNotSubtaskAndClonedLink = Predicates.and(isNotSubtaskIssueLink, isNotSpecifiedLinkType(clonerIssueLinkType));

        final Iterable<IssueLink> inwardLinks = Iterables.filter(issueLinkManager.getInwardLinks(localIssue.getId()), isNotSubtaskAndClonedLink);
        for (final IssueLink inwardLink : inwardLinks) {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId, type,
                    JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        }

        final Iterable<IssueLink> outwardLinks = Iterables.filter(issueLinkManager.getOutwardLinks(localIssue.getId()), isNotSubtaskAndClonedLink);
        for (final IssueLink outwardLink : outwardLinks) {

            final IssueLinkType type = outwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type,
                    JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        }
    }
}
