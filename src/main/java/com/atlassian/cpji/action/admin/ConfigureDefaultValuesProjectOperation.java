package com.atlassian.cpji.action.admin;

import com.atlassian.jira.plugin.projectoperation.PluggableProjectOperation;
import com.atlassian.jira.plugin.projectoperation.ProjectOperationModuleDescriptor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.opensymphony.user.User;

import static java.lang.String.format;

/**
 * @since v1.4
 */
public class ConfigureDefaultValuesProjectOperation implements PluggableProjectOperation
{
    private ProjectOperationModuleDescriptor descriptor;
    private final PermissionManager permissionManager;
    private final VelocityRequestContextFactory velocityRequestContext;

    public ConfigureDefaultValuesProjectOperation(PermissionManager permissionManager, VelocityRequestContextFactory velocityRequestContext)
    {
        this.permissionManager = permissionManager;
        this.velocityRequestContext = velocityRequestContext;
    }

    public void init(ProjectOperationModuleDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    private static final String TEMPLATE = "<span class=\"project-config-list-label\">%s</span><a href=\"%s/secure/ConfigureCopyIssuesAdminAction.jspa?projectKey=%s\" id=\"configure_cpji\">%s</a>";

    final public String getHtml(Project project, User user)
    {
        return format(TEMPLATE, getLabelHtml(project, user), velocityRequestContext.getJiraVelocityRequestContext().getBaseUrl(), project.getKey(), getContentHtml(project, user));
    }

    public boolean showOperation(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.PROJECT_ADMIN, project, user) || permissionManager.hasPermission(Permissions.ADMINISTER, user);
    }

    public String getLabelHtml(Project project, User user)
    {
        return "Copy JIRA Issue(s)";
    }

    public String getContentHtml(Project project, User user)
    {
        return "Configure permission and default field values";
    }
}
