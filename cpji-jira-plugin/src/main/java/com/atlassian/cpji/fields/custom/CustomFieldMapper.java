package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.CustomFieldMappingResult;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * Maps between CustomFields (including its value) and CustomFieldBeans.
 *
 * @since v1.4
 */
public interface CustomFieldMapper
{
    /**
     * Returns the canonical name of the class of the custom field type we are mapping.
     *
     * @return the canonical name of the class of the custom field type we are mapping
     */
    String getType();

    /**
     * Create a CustomFieldBean representing the given CustomField and its value in the given Issue.
     * 
     * @param customField the CustomField
     * @param issue the Issue
     * @return a CustomFieldBean
     */
    CustomFieldBean createFieldBean(CustomField customField, Issue issue);

    /**
     * Generates a MappingResult, to discover information about mapping a custom field (represented by the given
     * CustomFieldBean) to the CustomField, Project and IssueType of the current JIRA instance.
     *
     * @param customFieldBean the incoming custom field
     * @param customField the destination CustomField on this JIRA instance
     * @param project the destination Project field on this JIRA instance
     * @param issueType the destination IssueType on this JIRA instance
     * @return a MappingResult
     */
    public CustomFieldMappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType);

    /**
     * Applies the values in the given CustomFieldBean to the given IssueInputParameters for the CustomField, Project
     * and IssueType in the current JIRA instance.
     * 
     * @param inputParameters the input parameters to apply the custom field values to
     * @param mappingResult the CustomFieldMappingResult containing the valid and invalid custom field values
     * @param customField the CustomField on this JIRA instance to set the value of for the new issue
     * @param project the Project on this JIRA instance in which the new issue is being created
     * @param issueType the IssueType of the issue to be created on this JIRA instance
     */
    public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldMappingResult mappingResult, final CustomField customField, final Project project, final IssueType issueType);
}
