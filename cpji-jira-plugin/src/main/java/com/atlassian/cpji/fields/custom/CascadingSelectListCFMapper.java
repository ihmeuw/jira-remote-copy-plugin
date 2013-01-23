package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.CustomFieldMappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @since v1.4
 */
public class CascadingSelectListCFMapper implements CustomFieldMapper {
	@Override
	public boolean acceptsType(CustomFieldType<?, ?> type) {
		return type instanceof CascadingSelectCFType;
	}

	@Override
	public CustomFieldBean createFieldBean(final CustomField customField, final Issue issue)
	{
		List<String> values;

		final Object valueObject = customField.getValue(issue);
		if (valueObject != null)
		{
			try
			{
				final Map<String, Option> value = (Map<String, Option>) valueObject;
				final Option parent = value.get(CascadingSelectCFType.PARENT_KEY);
				final Option child = value.get(CascadingSelectCFType.CHILD_KEY);
				values = Lists.newArrayList();
				if (parent != null) {
					values.add(parent.getValue());
					if (child != null) {
						values.add(child.getValue());
					}
				}
			}
			catch (final ClassCastException e)
			{
				// Value is unrecognised type, ignore it
				values = Collections.emptyList();
			}
		}
		else
		{
			// Value is null, ignore it
			values = Collections.emptyList();
		}

		final String customFieldType = customField.getCustomFieldType().getClass().getCanonicalName();
		return new CustomFieldBean(customFieldType, customField.getName(), customField.getId(), values);
	}

	@Override
	public CustomFieldMappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
	{
		final List<String> validValues;
		final List<String> invalidValues;

		final List<String> value = customFieldBean.getValues();
		if (value == null)
		{
			validValues = Collections.emptyList();
			invalidValues = Collections.emptyList();
		}
		else if (isValidValue(value, customField, project, issueType))
		{
			validValues = value;
			invalidValues = Collections.emptyList();
		}
		else
		{
			validValues = Collections.emptyList();
			invalidValues = value;
		}

		return new CustomFieldMappingResult(validValues, invalidValues);
	}

	@Override
	public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldMappingResult mappingResult, final CustomField customField, final Project project, final IssueType issueType)
	{
		if (mappingResult.getValidValues().size() > 0) {
			final String parent = mappingResult.getValidValues().get(0);
			final String child = mappingResult.getValidValues().size() > 1 ? mappingResult.getValidValues().get(1) : null;
			if (parent != null)
			{
				final Option parentOption = getOption(parent, null, customField, project, issueType);
				final Option childOption = child != null && parentOption != null ? getOption(child, parentOption.getOptionId(), customField, project, issueType) : null;
				if (parentOption != null) {
					inputParameters.addCustomFieldValue(customField.getId(), parentOption.getOptionId().toString());
					if (childOption != null) {
						inputParameters.addCustomFieldValue(customField.getId() + ":1", childOption.getOptionId().toString());
					}
				}
			}
		}
	}

    protected boolean isValidValue(final List<String> parentAndChild, final CustomField customField, final Project project, final IssueType issueType)
    {
		if (parentAndChild.isEmpty()) {
			return true;
		}
        final Option parentOption = getOption(parentAndChild.get(0), null, customField, project, issueType);
		if (parentAndChild.size() > 1 && parentOption != null) {
			final Option childOption = getOption(parentAndChild.get(1), parentOption.getOptionId(), customField, project, issueType);
			return childOption != null;
		} else {
			return parentOption != null;
		}
    }

	@Nullable
    private Option getOption(final String value, final Long parentId, final CustomField customField, final Project project, final IssueType issueType)
    {
        final FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        final Options options = customField.getOptions("notUsed", fieldConfig, null);
        return options.getOptionForValue(value, parentId);
    }
}
