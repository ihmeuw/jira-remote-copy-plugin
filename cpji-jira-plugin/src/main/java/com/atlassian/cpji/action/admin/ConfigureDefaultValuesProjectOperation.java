package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.action.AbstractCopyIssueAction;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.util.CalendarResourceIncluder;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.collect.ImmutableMap;

import java.util.Locale;

/**
 * @since v1.4
 */
public class ConfigureDefaultValuesProjectOperation extends AbstractPluggableProjectOperation
{
    private final PermissionManager permissionManager;
    private final WebResourceManager webResourceManager;
	private final JiraAuthenticationContext authenticationContext;

	public ConfigureDefaultValuesProjectOperation(PermissionManager permissionManager, WebResourceManager webResourceManager,
			JiraAuthenticationContext authenticationContext)
    {
        this.permissionManager = permissionManager;
        this.webResourceManager = webResourceManager;
		this.authenticationContext = authenticationContext;
	}

    final public String getHtml(Project project, ApplicationUser user)
    {
		final Locale locale = authenticationContext.getLocale();
		final CalendarResourceIncluder calendarResourceIncluder = new CalendarResourceIncluder();
		calendarResourceIncluder.includeForLocale(locale);

        webResourceManager.requireResource(AbstractCopyIssueAction.RESOURCES_ADMIN_JS);
        ImmutableMap<String, ?> params = ImmutableMap.of(
                "projectKey", project.getKey()
        );
        return descriptor.getHtml("view", params);
    }

    public boolean showOperation(final Project project, final ApplicationUser user)
    {
        return permissionManager.hasPermission(Permissions.PROJECT_ADMIN, project, user) || permissionManager.hasPermission(Permissions.ADMINISTER, user);
    }

}
