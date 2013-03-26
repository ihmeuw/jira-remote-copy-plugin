package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public abstract class AbstractSystemFieldMapper implements FieldMapper, IssueCreationFieldMapper {
	protected final DefaultFieldValuesManager defaultFieldValuesManager;

    final String nameKey;
    final String id;

    public AbstractSystemFieldMapper(Field field, DefaultFieldValuesManager defaultFieldValuesManager)
    {
		this.defaultFieldValuesManager = defaultFieldValuesManager;
		this.nameKey = field.getNameKey();
        this.id = field.getId();
    }

    public String getFieldId()
    {
        return id;
    }

    public String getFieldNameKey()
    {
        return nameKey;
    }

	/**
	 * It will return true if the mapper detects there's a default value configured in the destination server. Either the value is configured
	 * manually in project configuration or it can be detected based on JIRA configuration.
	 *
	 * @return true if we can guess the default value
	 */
	boolean hasDefaultValue(Project project, CopyIssueBean copyIssueBean)
	{
		return defaultFieldValuesManager.hasDefaultValue(project.getKey(), getFieldId(),
				copyIssueBean.getTargetIssueType());
	}

	@Override
	public void populateInputParams(IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
		MappingResult mappingResult = getMappingResult(copyIssueBean, project);
		if (!mappingResult.hasOneValidValue() && fieldLayoutItem.isRequired()) {
			String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), getFieldId(), issueType.getName());
			if (defaultFieldValue != null) {
				inputParameters.getActionParameters().put(getFieldId(), defaultFieldValue);
			}
		} else {
			populateCurrentValue(inputParameters, copyIssueBean, fieldLayoutItem, project);
		}
	}

	abstract void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project);

}
