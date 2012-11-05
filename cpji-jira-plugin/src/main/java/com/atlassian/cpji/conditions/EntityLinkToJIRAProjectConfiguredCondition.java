package com.atlassian.cpji.conditions;

import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.applinks.api.application.jira.JiraProjectEntityType;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import org.apache.log4j.Logger;

/**
 * @since v1.0
 */
public class EntityLinkToJIRAProjectConfiguredCondition extends AbstractIssueCondition
{
    private static final Logger log = Logger.getLogger(EntityLinkToJIRAProjectConfiguredCondition.class);

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
        final boolean hasProjectLink = (entityLinkService.getEntityLinks(issue.getProjectObject(), JiraProjectEntityType.class).iterator().hasNext());
        final boolean hasPermissionForProject = copyIssuePermissionManager.hasPermissionForProject(issue.getProjectObject().getKey());
        log.debug("shouldDisplay for " + issue.getKey() + ": [hasProjectLink: " + hasProjectLink + ", hasPermissionForProject: " + hasPermissionForProject + "]");
        return hasProjectLink && hasPermissionForProject;
    }
}
