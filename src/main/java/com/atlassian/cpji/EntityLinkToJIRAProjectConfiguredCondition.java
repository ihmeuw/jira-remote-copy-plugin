package com.atlassian.cpji;

import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.applinks.api.application.jira.JiraProjectEntityType;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;

/**
 * @since v1.0
 */
public class EntityLinkToJIRAProjectConfiguredCondition extends AbstractIssueCondition
{
    private final EntityLinkService entityLinkService;
    private final CopyIssuePermissionManager copyIssuePermissionManager;

    public EntityLinkToJIRAProjectConfiguredCondition(EntityLinkService entityLinkService, final CopyIssuePermissionManager copyIssuePermissionManager)
    {
        this.entityLinkService = entityLinkService;
        this.copyIssuePermissionManager = copyIssuePermissionManager;
    }

    @Override
    public boolean shouldDisplay(final User user, final Issue issue, final JiraHelper jiraHelper)
    {
        return (entityLinkService.getPrimaryEntityLink(issue.getProjectObject(), JiraProjectEntityType.class) != null) && copyIssuePermissionManager.hasPermissionForProject(issue.getProjectObject().getKey());
    }
}
