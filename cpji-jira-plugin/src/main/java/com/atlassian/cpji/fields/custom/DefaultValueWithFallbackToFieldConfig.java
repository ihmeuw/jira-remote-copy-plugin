package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultValueWithFallbackToFieldConfig implements CustomFieldDefaultValueEvaluationStrategy {

    private final DefaultFieldValuesManager defaultFieldValuesManager;

    public DefaultValueWithFallbackToFieldConfig(DefaultFieldValuesManager defaultFieldValuesManager) {
        this.defaultFieldValuesManager = defaultFieldValuesManager;
    }

    @Override
    public boolean hasDefaultValueDefined(CustomField customField, Project project, IssueType issueType) {
        return getDefaultValue(customField, project, issueType) != null;
    }

    String[] getDefaultValue(final CustomField customField, final Project project, final IssueType issueType) {
        //try to get from manager
        String[] defaultValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), customField.getId(),
                issueType.getName());
        if (defaultValue != null) {
            return defaultValue;
        }

        //fallback to field config
        final FieldConfig config = customField.getRelevantConfig(new IssueContextImpl(project, issueType));
        final Object contextDefaultValue = customField.getCustomFieldType().getDefaultValue(config);

        if (contextDefaultValue == null) {
            return null;
        }

        final List<Object> defaultValues;
        if (contextDefaultValue instanceof Iterable<?>) {
            defaultValues = Lists.newArrayList((Iterable<?>) contextDefaultValue);
        } else {
            defaultValues = ImmutableList.of(contextDefaultValue);
        }

        Iterable<String> serializedDefaultValues = Iterables.transform(defaultValues, new Function<Object, String>() {
            @Override
            public String apply(@Nullable Object input) {
                return customField.getCustomFieldType().getStringFromSingularObject(input);
            }
        });

        return Iterables.toArray(serializedDefaultValues, String.class);
    }

    @Override
    public void populateWithDefaultValue(final IssueInputParameters inputParameters, final CustomField customField, final Project project, final IssueType issueType) {

        final String[] defVal = getDefaultValue(customField, project, issueType);
        if (defVal != null) {
            inputParameters.addCustomFieldValue(customField.getId(), defVal);
        }


    }
}
