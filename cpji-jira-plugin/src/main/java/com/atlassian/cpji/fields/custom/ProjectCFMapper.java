package com.atlassian.cpji.fields.custom;

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
public class ProjectCFMapper extends AbstractSingleValueCFMapper<GenericValue>
{
	private final ProjectManager projectManager;

	public ProjectCFMapper(final ProjectManager projectManager)
    {
		this.projectManager = projectManager;
	}

    @Override
    public String getType()
    {
        return ProjectCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final GenericValue value)
    {
        return value.getString("name");
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
