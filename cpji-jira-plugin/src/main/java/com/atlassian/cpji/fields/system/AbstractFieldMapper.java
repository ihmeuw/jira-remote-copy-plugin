package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public abstract class AbstractFieldMapper implements FieldMapper
{
    final String nameKey;
    final String id;

    public AbstractFieldMapper(Field field)
    {
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
	boolean defaultValueConfigured(Project project, CopyIssueBean copyIssueBean)
	{
		return (getDefaultFieldValuesManager().getDefaultFieldValue(project.getKey(), getFieldId(),
				copyIssueBean.getTargetIssueType()) != null);
	}

	protected DefaultFieldValuesManager getDefaultFieldValuesManager() {
		return ComponentAccessor.getComponent(DefaultFieldValuesManager.class);
	}


}
