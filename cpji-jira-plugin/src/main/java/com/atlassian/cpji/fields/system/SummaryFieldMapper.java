package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.SummarySystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

import java.util.Collections;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 *
 * @since v1.4
 */
public class SummaryFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    public SummaryFieldMapper(FieldManager fieldManager, DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.SUMMARY), defaultFieldValuesManager);
    }

    public Class<? extends OrderableField> getField()
    {
        return SummarySystemField.class;
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
			inputParameters.setSummary(copyIssueBean.getSummary());
		}
	}

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        if (bean.getSummary() != null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
        }
        return new MappingResult(Collections.<String>emptyList(), false, true, hasDefaultValue(project, bean));
    }

}
