package com.atlassian.cpji.action.admin;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @since v1.5
 */
public class CopyIssuePermissionManager {
	private final PluginSettingsFactory pluginSettingsFactory;
	private final JiraAuthenticationContext jiraAuthenticationContext;
	private final GroupManager groupManager;

	public static final String CPJI_OLD_KEY = "cpji.permission.%s";

	public static final String ALLOWED_GROUPS_KEY = "cpji.allowed.groups";

	public CopyIssuePermissionManager(final PluginSettingsFactory pluginSettingsFactory, final JiraAuthenticationContext jiraAuthenticationContext, final GroupManager groupManager) {
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.jiraAuthenticationContext = jiraAuthenticationContext;
		this.groupManager = groupManager;
	}

	public void restrictPermissionToGroups(@Nonnull String projectKey, @Nonnull Iterable<String> groups) {
		PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
		settingsForProject.put(ALLOWED_GROUPS_KEY, ImmutableList.copyOf(groups));
	}

	public boolean hasPermissionForProject(String projectKey) {
		final List<String> groups = getConfiguredGroups(projectKey);
		if (groups.isEmpty()) {
			return true;
		}

		final User loggedInUser = jiraAuthenticationContext.getLoggedInUser();
		final Set<String> loggedInGroups = Sets.newHashSet(groupManager.getGroupNamesForUser(loggedInUser));
		for(String group : groups) {
			if(loggedInGroups.contains(group)) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @param projectKey
	 * @return list of group names or empty list if there are no groups configured
	 */
	@Nonnull
	public List<String> getConfiguredGroups(String projectKey) {
		PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
		List<String> groups = (List<String>) settingsForProject.get(ALLOWED_GROUPS_KEY);
		if (groups != null) {
			return groups;
		}

		// check if the old setting is set and migrate it if it's set
		final String oldKey = createOldKey(projectKey);
		final Object value = settingsForProject.get(oldKey);
		if (value != null) {
			restrictPermissionToGroups(projectKey, ImmutableList.of(value.toString()));
			settingsForProject.remove(oldKey);
			return getConfiguredGroups(projectKey);
		}
		return Collections.emptyList();
	}

	public void clearPermissionForProject(@Nonnull String projectKey) {
		PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
		settingsForProject.remove(ALLOWED_GROUPS_KEY);
		settingsForProject.remove(createOldKey(projectKey));
	}


	/*
	 * this key was used in versions prior 2.1 to save the one group that was allowed to use the copy feature
	 */
	@Nonnull
	private String createOldKey(@Nonnull final String projectKey) {
		return String.format(CPJI_OLD_KEY, projectKey);
	}
}
