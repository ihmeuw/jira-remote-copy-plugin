package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.action.AbstractCopyIssueAction;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.collect.ImmutableMap;

/**
 * @since v1.4
 */
public class ConfigureDefaultValuesProjectOperation extends AbstractPluggableProjectOperation
{
    private final PermissionManager permissionManager;
    private final WebResourceManager webResourceManager;

    public ConfigureDefaultValuesProjectOperation(PermissionManager permissionManager, WebResourceManager webResourceManager)
    {
        this.permissionManager = permissionManager;
        this.webResourceManager = webResourceManager;
    }

    final public String getHtml(Project project, User user)
    {
        webResourceManager.requireResource(AbstractCopyIssueAction.PLUGIN_KEY + ":admin-js");
        ImmutableMap<String, ?> params = ImmutableMap.of(
                "projectKey", project.getKey()
        );
        return descriptor.getHtml("view", params);
    }

    public boolean showOperation(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.PROJECT_ADMIN, project, user) || permissionManager.hasPermission(Permissions.ADMINISTER, user);
    }

}
