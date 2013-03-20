package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public abstract class AbstractFieldMapper implements FieldMapper
{
	protected final DefaultFieldValuesManager defaultFieldValuesManager;

    final String nameKey;
    final String id;

    public AbstractFieldMapper(Field field, DefaultFieldValuesManager defaultFieldValuesManager)
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

}
