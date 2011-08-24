package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public interface CustomFieldMapper
{
    String getType();

    CustomFieldBean createFieldBean(CustomField customField, Issue issue);

    public MappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType);

    public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType);


}
