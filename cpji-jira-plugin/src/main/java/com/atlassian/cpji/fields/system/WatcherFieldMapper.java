package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.watcher.WatcherService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @since v1.4
 */
public class WatcherFieldMapper extends AbstractFieldMapper implements PostIssueCreationFieldMapper, NonOrderableSystemFieldMapper
{
    private final WatcherService watcherService;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public WatcherFieldMapper(final WatcherService watcherService, final PermissionManager permissionManager,
			final JiraAuthenticationContext jiraAuthenticationContext, final Field field,
			final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(field, defaultFieldValuesManager);
        this.watcherService = watcherService;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }


    public void process(final CachingUserMapper userMapper, final Issue issue, final CopyIssueBean bean)
    {
        if (watcherService.isWatchingEnabled() && permissionManager.hasPermission(Permissions.MANAGE_WATCHER_LIST, issue, jiraAuthenticationContext.getLoggedInUser()))
        {
            List<UserBean> watchers = makeSureNotNull(bean.getWatchers());
            for (UserBean user : watchers)
            {
                ApplicationUser watcher = userMapper.mapUser(user);
                if (watcher != null)
                {
                    watcherService.addWatcher(issue, jiraAuthenticationContext.getLoggedInUser(), watcher);
                }
            }
        }
    }

    public boolean userHasRequiredPermission(final Project project, final ApplicationUser user)
    {
        return permissionManager.hasPermission(Permissions.MANAGE_WATCHER_LIST, project, user) && watcherService.isWatchingEnabled();
    }

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedValues = new ArrayList<String>();
        List<String> mappedValues = new ArrayList<String>();
        List<UserBean> watchers = makeSureNotNull(bean.getWatchers());
        if (watchers.isEmpty())
        {
            return new MappingResult(unmappedValues, true, true, hasDefaultValue(project, bean));
        }
        for (UserBean user : watchers)
        {
            ApplicationUser watcher = userMapper.mapUser(user);
            if (watcher == null)
            {
                unmappedValues.add(user.getUserName());
            } else {
                mappedValues.add(user.getUserName());
            }
        }
        return new MappingResult(unmappedValues, unmappedValues.isEmpty(), false, hasDefaultValue(project, bean));
    }

    private <T>List makeSureNotNull(List<T> inputList)
    {
        return (inputList == null) ? Lists.newArrayList() : inputList;
    }

    public boolean isVisible()
    {
        return watcherService.isWatchingEnabled();
    }
}
