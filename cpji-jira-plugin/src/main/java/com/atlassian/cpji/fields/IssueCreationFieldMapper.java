package com.atlassian.cpji.fields;

import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public interface IssueCreationFieldMapper extends SystemFieldMapper
{
	Class<? extends OrderableField>  getField();

    void populateInputParams(IssueInputParameters inputParameters, CopyIssueBean bean, FieldLayoutItem fieldLayoutItem,
			final Project project, IssueType issueType);
}
