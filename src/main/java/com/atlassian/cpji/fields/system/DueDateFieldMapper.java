package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.DueDateSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * @since v1.4
 */
public class DueDateFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final PermissionManager permissionManager;

    public DueDateFieldMapper(final PermissionManager permissionManager, final Field field)
    {
        super(field);
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return DueDateSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        Date issueDueDate = bean.getIssueDueDate();
        if (issueDueDate != null)
        {
            SimpleDateFormat dueDateFormat = new SimpleDateFormat("d/MMM/yy");
            String dueDate = dueDateFormat.format(issueDueDate);
            inputParameters.setDueDate(dueDate);
        }
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.SCHEDULE_ISSUE, project, user);
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        Date issueDueDate = bean.getIssueDueDate();
        if (issueDueDate != null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, false);
        }
        else
        {
            return new MappingResult(Collections.<String>emptyList(), false, true);
        }
    }


}
