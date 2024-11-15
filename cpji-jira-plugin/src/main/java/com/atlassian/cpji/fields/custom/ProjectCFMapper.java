package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.ProjectCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import org.ofbiz.core.entity.GenericValue;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class ProjectCFMapper extends AbstractSingleValueCFMapper<Project>
{
	private final ProjectManager projectManager;

	public ProjectCFMapper(final ProjectManager projectManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		super(defaultFieldValuesManager);
		this.projectManager = projectManager;
	}

    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof ProjectCFType;
    }

    @Override
    protected String convertToString(final Project value)
    {
        return value.getName();
    }

	@Override
	protected String formatString(String value, CustomField customField, Project project, IssueType issueType) {
		return projectManager.getProjectObjByName(value).getId().toString();
	}

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return projectManager.getProjectObjByName(value) != null;
    }
}
