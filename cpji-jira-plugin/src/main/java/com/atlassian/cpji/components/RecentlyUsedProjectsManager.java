package com.atlassian.cpji.components;

import com.atlassian.cpji.action.SelectedProject;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.0
 */
public class RecentlyUsedProjectsManager {
	public static final int REMEMBER_COUNT = 10;
	protected final Logger log = Logger.getLogger(this.getClass());

	public static final String CPJI_RECENTLY_USED_PROJECTS = "cpji.recently.used.projects";

	private final PluginSettingsFactory pluginSettingsFactory;

	public RecentlyUsedProjectsManager(PluginSettingsFactory pluginSettingsFactory) {
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	public void addProject(ApplicationUser user, SelectedProject project) {
		PluginSettings settings = pluginSettingsFactory
				.createSettingsForKey("user." + checkNotNull(user).getKey());

		List<SelectedProject> recentlyUsed = Lists.newArrayList(getRecentProjects(user));
		if (recentlyUsed.contains(project)) {
			recentlyUsed.remove(project);
		}
		recentlyUsed.add(0, project);
		if (recentlyUsed.size() > REMEMBER_COUNT) {
			recentlyUsed = recentlyUsed.subList(0, REMEMBER_COUNT);
		}
		try {
			settings.put(CPJI_RECENTLY_USED_PROJECTS, new ObjectMapper().writeValueAsString(recentlyUsed));
		} catch (IOException e) {
			log.debug("Unable to write recently used projects", e);
		}
	}

	/**
	 *
	 * @param user
	 * @return immutable list of recently selected projects
	 */
	@Nonnull
	public List<SelectedProject> getRecentProjects(ApplicationUser user) {
		PluginSettings settings = pluginSettingsFactory
				.createSettingsForKey("user." + checkNotNull(user).getKey());
		List<SelectedProject> recentlyUsed = null;
		try {
			recentlyUsed = new ObjectMapper().readValue(
					(String) settings.get(CPJI_RECENTLY_USED_PROJECTS),
					new TypeReference<List<SelectedProject>>() {});
		} catch (Exception e) {
			log.debug("Unable to read recently used projects", e);
		}
		return recentlyUsed == null ? Collections.<SelectedProject>emptyList() : recentlyUsed;
	}
}
