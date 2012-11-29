package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.JiraLocation;
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

    public JiraProxyFactory(ApplicationLinkService applicationLinkService, PermissionManager permissionManager, JiraAuthenticationContext jiraAuthenticationContext, InternalHostApplication hostApplication) {
        this.applicationLinkService = applicationLinkService;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.hostApplication = hostApplication;
    }

    public JiraProxy createJiraProxy(JiraLocation jira) {
        if (LocalJiraProxy.LOCAL_JIRA_LOCATION.equals(jira))
            return new LocalJiraProxy(permissionManager, jiraAuthenticationContext);
        else {
            try {
                final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(jira.getId()));
                return new RemoteJiraProxy(hostApplication, applicationLink, jira);
            } catch (TypeNotInstalledException e) {
                return null;
            }

        }
    }


    public Iterable<JiraProxy> getAllJiraProxies(){
        return Iterables.concat(getRemoteProxies(), ImmutableList.of(createJiraProxy(LocalJiraProxy.LOCAL_JIRA_LOCATION)));
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
