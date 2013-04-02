package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

public abstract class AbstractCustomFieldMapper implements CustomFieldMapper {

    protected final CustomFieldDefaultValueEvaluationStrategy customFieldDefaultValueEvaluationStrategy;

    public AbstractCustomFieldMapper(CustomFieldDefaultValueEvaluationStrategy customFieldDefaultValueEvaluationStrategy) {
        this.customFieldDefaultValueEvaluationStrategy = customFieldDefaultValueEvaluationStrategy;
    }

    public AbstractCustomFieldMapper(DefaultFieldValuesManager defaultFieldValuesManager){
        customFieldDefaultValueEvaluationStrategy = new DefaultValueWithFallbackToFieldConfig(defaultFieldValuesManager);
    }


    @Override
    public void populateWithDefaultValue(IssueInputParameters inputParameters, CustomField customField, Project project, IssueType issueType) {
        customFieldDefaultValueEvaluationStrategy.populateWithDefaultValue(inputParameters, customField, project, issueType);
    }

    @Override
    public boolean hasDefaultValueDefined(CustomField customField, Project project, IssueType issueType) {
        return customFieldDefaultValueEvaluationStrategy.hasDefaultValueDefined(customField, project, issueType);
    }
}
