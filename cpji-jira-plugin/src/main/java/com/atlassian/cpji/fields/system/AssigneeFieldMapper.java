package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @since v1.4
 */
public class AssigneeFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;
    private final UserMappingManager userMappingManager;

    public AssigneeFieldMapper(PermissionManager permissionManager, final ApplicationProperties applicationProperties, Field field, final UserMappingManager userMappingManager)
    {
        super(field);
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
        this.userMappingManager = userMappingManager;
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
        if (bean.getAssignee() != null)
        {
            final User assignee = findUser(bean.getAssignee(), project);
            if (assignee == null)
            {
                unmappedFieldValues.add(bean.getAssignee().getUserName());
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
                    unmappedFieldValues.add(bean.getAssignee().getUserName());
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
        if (bean.getAssignee() != null)
        {
            final User assignee = findUser(bean.getAssignee(), project);
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

    private User findUser(final UserBean user, final Project project)
    {
        return userMappingManager.mapUser(user, project);
    }
}
