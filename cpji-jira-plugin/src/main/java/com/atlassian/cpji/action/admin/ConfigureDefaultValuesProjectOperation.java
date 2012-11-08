package com.atlassian.cpji.action.admin;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.ImmutableMap;

/**
 * @since v1.4
 */
public class ConfigureDefaultValuesProjectOperation extends AbstractPluggableProjectOperation
{
    private final PermissionManager permissionManager;

    public ConfigureDefaultValuesProjectOperation(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    final public String getHtml(Project project, User user)
    {

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
