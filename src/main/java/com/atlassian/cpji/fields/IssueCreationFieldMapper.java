package com.atlassian.cpji.fields;

import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public interface IssueCreationFieldMapper extends FieldMapper
{
    void populateInputParameters(IssueInputParameters inputParameters, CopyIssueBean bean, FieldLayoutItem fieldLayoutItem, final Project project);

}
