package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.util.UserManager;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @since v1.4
 */
public class AssigneeFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final UserManager userManager;
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;

    public AssigneeFieldMapper(UserManager userManager, PermissionManager permissionManager, final ApplicationProperties applicationProperties, Field field)
    {
        super(field);
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
    }

    public Class<? extends OrderableField> getField()
    {
        return AssigneeSystemField.class;
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.ASSIGN_ISSUE, project, user);
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
        List<String> unmappedFieldValues = new ArrayList<String>();
        if (!StringUtils.isEmpty(bean.getAssignee()))
        {
            final User assignee = findUser(bean.getAssignee());
            if (assignee == null)
            {
                unmappedFieldValues.add(bean.getAssignee());
                if (unassignedAllowed)
                {
                    return new MappingResult(unmappedFieldValues, true, false);
                }
                return new MappingResult(unmappedFieldValues, false, false);
            }
            else
            {
                if (!permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, assignee))
                {
                    unmappedFieldValues.add(bean.getAssignee());
                    return new MappingResult(unmappedFieldValues, false, false);
                }
                return new MappingResult(unmappedFieldValues, true, false);
            }
        }
        if (unassignedAllowed)
        {
            return new MappingResult(unmappedFieldValues, true, true);
        }
        return new MappingResult(unmappedFieldValues, false, true);
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        if (!StringUtils.isEmpty(bean.getAssignee()))
        {
            final User assignee = findUser(bean.getAssignee());
            if (assignee != null)
            {
                if (permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, assignee))
                {
                    inputParameters.setAssigneeId(assignee.getName());
                }
            }
            else if (!applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED))
            {
                inputParameters.setAssigneeId(project.getLeadUserName());
            }
        }
        else if (!applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED))
        {
            inputParameters.setAssigneeId(project.getLeadUserName());
        }
    }

    private User findUser(final String username)
    {
        return userManager.getUserObject(username);
    }
}
