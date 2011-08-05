package com.atlassian.cpji.action.admin;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * @since v1.5
 */
public class CopyIssuePermissionManager
{
    private final PluginSettingsFactory pluginSettingsFactory;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final GroupManager groupManager;

    public static final String CPJI_DEFAULT_KEY = "cpji.permission.%s";

    public CopyIssuePermissionManager(final PluginSettingsFactory pluginSettingsFactory, final JiraAuthenticationContext jiraAuthenticationContext, final GroupManager groupManager)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.groupManager = groupManager;
    }

    public void restrictPermissionToGroup(String projectKey, String groupName)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        settingsForProject.put(createKey(projectKey), groupName);
    }

    public boolean hasPermissionForProject(String projectKey)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        Object value = settingsForProject.get(createKey(projectKey));
        if (value != null)
        {
            String group = (String) value;
            Group groupObject = groupManager.getGroupObject(group);
            if (groupObject == null)
            {
                return false;
            }
            User loggedInUser = jiraAuthenticationContext.getLoggedInUser();
            return groupManager.isUserInGroup(loggedInUser, groupObject);
        }
        return true;
    }

    public String getConfiguredGroup(String projectKey)
    {
       PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
       Object value = settingsForProject.get(createKey(projectKey));
       if (value != null)
       {
           return (String) value;
       }
       return null;
    }

    public void clearPermissionForProject(String projectKey)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        settingsForProject.remove(createKey(projectKey));
    }


    private String createKey(final String projectKey)
    {
        return String.format(CPJI_DEFAULT_KEY, projectKey);
    }
}
