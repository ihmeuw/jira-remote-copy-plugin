package com.atlassian.cpji.conditions;

import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import org.apache.log4j.Logger;

/**
 * @since v2.2
 */
public class HasCopyPermissionCondition extends AbstractIssueCondition
{
    private static final Logger log = Logger.getLogger(HasCopyPermissionCondition.class);

    private final CopyIssuePermissionManager copyIssuePermissionManager;

    public HasCopyPermissionCondition(final CopyIssuePermissionManager copyIssuePermissionManager)
    {
        this.copyIssuePermissionManager = copyIssuePermissionManager;
    }

    @Override
    public boolean shouldDisplay(final User user, final Issue issue, final JiraHelper jiraHelper)
    {
        final boolean hasPermissionForProject = copyIssuePermissionManager.hasPermissionForProject(issue.getProjectObject().getKey());
        log.debug("shouldDisplay for " + issue.getKey() + ": [hasPermissionForProject: " + hasPermissionForProject + "]");
        return hasPermissionForProject;
    }
}
