package com.atlassian.cpji.conditions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;

/**
 * @since v3.0
 */
public class HasCreateIssuePermissionInAnyProjectCondition extends AbstractIssueCondition {

    private static final Logger log = Logger.getLogger(HasCreateIssuePermissionInAnyProjectCondition.class);

    private final PermissionManager permissionManager;

    public HasCreateIssuePermissionInAnyProjectCondition(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    public boolean shouldDisplay(final ApplicationUser user, final Issue issue, final JiraHelper jiraHelper) {
        final boolean canCreateInLocalProjects = !permissionManager.getProjects(ProjectPermissions.CREATE_ISSUES, user).isEmpty();
        log.debug("shouldDisplay for " + issue.getKey() + ": [canCreateInLocalProjects: " + canCreateInLocalProjects + "]");
        return canCreateInLocalProjects;
    }
}
