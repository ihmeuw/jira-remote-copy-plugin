package com.atlassian.cpji.conditions;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

/**
 * @since v2.2
 */
public class JiraApplicationLinkIsConfiguredCondition extends AbstractIssueCondition
{
    private static final Logger log = Logger.getLogger(JiraApplicationLinkIsConfiguredCondition.class);

    private final ApplicationLinkService applicationLinkService;

    public JiraApplicationLinkIsConfiguredCondition(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    @Override
    public boolean shouldDisplay(final ApplicationUser user, final Issue issue, final JiraHelper jiraHelper)
    {
        final boolean hasJiraLink = !Iterables.isEmpty(applicationLinkService.getApplicationLinks(JiraApplicationType.class));
        log.debug("shouldDisplay for " + issue.getKey() + ": [hasJiraLink: " + hasJiraLink + "]");
        return hasJiraLink;
    }
}
