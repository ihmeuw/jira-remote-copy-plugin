package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.DueDateSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class DueDateFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final PermissionManager permissionManager;

    public DueDateFieldMapper(final PermissionManager permissionManager, final FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.DUE_DATE), defaultFieldValuesManager);
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return DueDateSystemField.class;
    }

	@Override
	public void populateInputParams(CachingUserMapper userMapper, IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
		MappingResult mappingResult = getMappingResult(userMapper, copyIssueBean, project);
		if (!mappingResult.hasOneValidValue() && fieldLayoutItem.isRequired()) {
			String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), getFieldId(), issueType.getName());
			if (defaultFieldValue != null) {
				inputParameters.getActionParameters().put(getFieldId(), defaultFieldValue);
			}
		} else {
			populateCurrentValue(inputParameters, copyIssueBean, fieldLayoutItem, project);
		}
	}

    public void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
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

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        Date issueDueDate = bean.getIssueDueDate();
        if (issueDueDate != null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
        }
        else
        {
            return new MappingResult(Collections.<String>emptyList(), false, true, hasDefaultValue(project, bean));
        }
    }


}
