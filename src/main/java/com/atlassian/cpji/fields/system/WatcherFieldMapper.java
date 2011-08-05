package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.bc.issue.watcher.WatcherService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @since v1.4
 */
public class WatcherFieldMapper extends AbstractFieldMapper implements SystemFieldPostIssueCreationFieldMapper, NonOrderableSystemFieldMapper
{
    private final WatcherService watcherService;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserManager userManager;

    public WatcherFieldMapper(final WatcherService watcherService, final PermissionManager permissionManager, final JiraAuthenticationContext jiraAuthenticationContext, final UserManager userManager, final Field field)
    {
        super(field);
        this.watcherService = watcherService;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userManager = userManager;
    }


    public void process(final Issue issue, final CopyIssueBean bean)
    {
        if (watcherService.isWatchingEnabled() && permissionManager.hasPermission(Permissions.MANAGE_WATCHER_LIST, issue, jiraAuthenticationContext.getLoggedInUser()))
        {
            List<String> watchers = makeSureNotNull(bean.getWatchers());
            for (String username : watchers)
            {
                User watcher = findUser(username);
                if (watcher != null)
                {
                    ServiceOutcome<List<User>> serviceOutcome = watcherService.addWatcher(issue, jiraAuthenticationContext.getLoggedInUser(), watcher);
                }
            }
        }
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.MANAGE_WATCHER_LIST, project, user) && watcherService.isWatchingEnabled();
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedValues = new ArrayList<String>();
        List<String> mappedValues = new ArrayList<String>();
        List<String> watchers = makeSureNotNull(bean.getWatchers());
        if (watchers.isEmpty())
        {
            return new MappingResult(unmappedValues, true, true);
        }
        for (String username : watchers)
        {
            User watcher = findUser(username);
            if (watcher == null)
            {
                unmappedValues.add(username);
            }
        }
        return new MappingResult(unmappedValues, !mappedValues.isEmpty(), false);
    }

    private <T> List makeSureNotNull(List<T> inputList)
    {
        return (inputList == null) ? Lists.newArrayList() : inputList;
    }

    private User findUser(final String username)
    {
        return userManager.getUserObject(username);
    }

    public boolean isVisible()
    {
        return watcherService.isWatchingEnabled();
    }
}
