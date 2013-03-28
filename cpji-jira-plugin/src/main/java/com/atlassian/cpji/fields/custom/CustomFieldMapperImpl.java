package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

public abstract class CustomFieldMapperImpl implements CustomFieldMapper {

    protected final CustomFieldDefaultValueEvaluationStrategy customFieldDefaultValueEvaluationStrategy;

    public CustomFieldMapperImpl(CustomFieldDefaultValueEvaluationStrategy customFieldDefaultValueEvaluationStrategy) {
        this.customFieldDefaultValueEvaluationStrategy = customFieldDefaultValueEvaluationStrategy;
    }

    public CustomFieldMapperImpl(DefaultFieldValuesManager defaultFieldValuesManager){
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
