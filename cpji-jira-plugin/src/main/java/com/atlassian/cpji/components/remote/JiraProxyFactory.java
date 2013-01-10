package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;

/**
 * @since v3.0
 */
public class JiraProxyFactory {

    private final ApplicationLinkService applicationLinkService;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final InternalHostApplication hostApplication;
    private final IssueLinkClient issueLinkClient;
    private final CopyIssueService copyIssueService;
    private final AttachmentManager attachmentManager;
    private final IssueManager issueManager;
    private final IssueLinkManager issueLinkManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final ProjectInfoService projectInfoService;
    private final JiraBaseUrls jiraBaseUrls;
    private final ApplicationProperties applicationProperties;

    public JiraProxyFactory(ApplicationLinkService applicationLinkService, PermissionManager permissionManager, JiraAuthenticationContext jiraAuthenticationContext, InternalHostApplication hostApplication, IssueLinkClient issueLinkClient, CopyIssueService copyIssueService, AttachmentManager attachmentManager, IssueManager issueManager, IssueLinkManager issueLinkManager, RemoteIssueLinkManager remoteIssueLinkManager, ProjectInfoService projectInfoService, JiraBaseUrls jiraBaseUrls, ApplicationProperties applicationProperties) {
        this.applicationLinkService = applicationLinkService;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.hostApplication = hostApplication;
        this.issueLinkClient = issueLinkClient;
        this.copyIssueService = copyIssueService;
        this.attachmentManager = attachmentManager;
        this.issueManager = issueManager;
        this.issueLinkManager = issueLinkManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.projectInfoService = projectInfoService;
        this.jiraBaseUrls = jiraBaseUrls;
        this.applicationProperties = applicationProperties;
    }

    public JiraProxy createJiraProxy(JiraLocation jira) {
        if (JiraLocation.isLocalLocation().apply(jira))
            return new LocalJiraProxy(permissionManager, jiraAuthenticationContext, copyIssueService, attachmentManager, issueManager, issueLinkManager, remoteIssueLinkManager, projectInfoService, jiraBaseUrls, applicationProperties);
        else {
            try {
                final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(jira.getId()));
                return new RemoteJiraProxy(hostApplication, applicationLink, jira, issueLinkClient, jiraAuthenticationContext);
            } catch (TypeNotInstalledException e) {
                return null;
            }

        }
    }

    public JiraLocation getLocationById(String id) {
        if (LocalJiraProxy.LOCAL_JIRA_LOCATION.getId().equals(id)) {
            return LocalJiraProxy.LOCAL_JIRA_LOCATION;
        } else {
            try {
                ApplicationLink link = applicationLinkService.getApplicationLink(new ApplicationId(id));
                return JiraLocation.fromAppLink(link);
            } catch (TypeNotInstalledException e) {
                return null;
            }

        }
    }


    public Iterable<JiraProxy> getAllJiraProxies() {
        return Iterables.concat(ImmutableList.of(createJiraProxy(LocalJiraProxy.LOCAL_JIRA_LOCATION)), getRemoteProxies());
    }

    public Iterable<JiraProxy> getRemoteProxies() {
        Iterable<ApplicationLink> remoteJiras = applicationLinkService.getApplicationLinks(JiraApplicationType.class);
        return Iterables.transform(remoteJiras, new Function<ApplicationLink, JiraProxy>() {
            @Override
            public JiraProxy apply(@Nullable final ApplicationLink input) {
                return createJiraProxy(JiraLocation.fromAppLink(input));
            }
        });
    }


}
