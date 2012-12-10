package com.atlassian.cpji.conditions;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

/**
 * @since v2.2
 */
public class JiraApplicationLinkIsConfiguredOrCanCreateIssuesCondition extends AbstractIssueCondition
{
    private static final Logger log = Logger.getLogger(JiraApplicationLinkIsConfiguredOrCanCreateIssuesCondition.class);

    private final ApplicationLinkService applicationLinkService;
    private final PermissionManager permissionManager;

    public JiraApplicationLinkIsConfiguredOrCanCreateIssuesCondition(ApplicationLinkService applicationLinkService, PermissionManager permissionManager)
    {
        this.applicationLinkService = applicationLinkService;
        this.permissionManager = permissionManager;
    }

    @Override
    public boolean shouldDisplay(final User user, final Issue issue, final JiraHelper jiraHelper)
    {
        final boolean hasJiraLink = !Iterables.isEmpty(applicationLinkService.getApplicationLinks(JiraApplicationType.class));
        final boolean canCreateInLocalProjects = !permissionManager.getProjectObjects(Permissions.CREATE_ISSUE, user).isEmpty();
        log.debug("shouldDisplay for " + issue.getKey() + ": [hasJiraLink: " + hasJiraLink + ", canCreateInLocalProjects: "+canCreateInLocalProjects +"]");
        return hasJiraLink || canCreateInLocalProjects;
    }
}
