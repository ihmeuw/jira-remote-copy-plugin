package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.CustomFieldMappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.fugue.Pair;
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

import java.util.Arrays;
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
				values = Lists.newArrayList(value.get(CascadingSelectCFType.PARENT_KEY).getValue(), value.get(CascadingSelectCFType.CHILD_KEY).getValue());
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

		final Pair<String,String> value = getValue(customFieldBean.getValues());
		if (value == null)
		{
			validValues = Collections.emptyList();
			invalidValues = Collections.emptyList();
		}
		else if (isValidValue(value, customField, project, issueType))
		{
			validValues = Arrays.asList(value.left(), value.right());
			invalidValues = Collections.emptyList();
		}
		else
		{
			validValues = Collections.emptyList();
			invalidValues = Arrays.asList(value.left(), value.right());
		}

		return new CustomFieldMappingResult(validValues, invalidValues);
	}

	@Override
	public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldMappingResult mappingResult, final CustomField customField, final Project project, final IssueType issueType)
	{
		final Pair<String,String> value = Pair.pair(mappingResult.getValidValues().get(0), mappingResult.getValidValues().get(1));
		if (value != null)
		{
			final Option parent = getOption(value.left(), null, customField, project, issueType);
			final Option child = getOption(value.right(), parent.getOptionId(), customField, project, issueType);
			inputParameters.addCustomFieldValue(customField.getId(), parent.getOptionId().toString());
			inputParameters.addCustomFieldValue(customField.getId() + ":1", child.getOptionId().toString());
		}
	}

    protected boolean isValidValue(final Pair<String,String> parentAndChild, final CustomField customField, final Project project, final IssueType issueType)
    {
        final Option parent = getOption(parentAndChild.left(), null, customField, project, issueType);
		final Option child = getOption(parentAndChild.right(), parent.getOptionId(), customField, project, issueType);
        return (parent != null) && child != null;
    }

    private Option getOption(final String value, final Long parentId, final CustomField customField, final Project project, final IssueType issueType)
    {
        final FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        final Options options = customField.getOptions("notUsed", fieldConfig, null);
        return options.getOptionForValue(value, parentId);
    }

	private Pair<String, String> getValue(final List<String> values)
	{
		if (values == null || values.isEmpty() || values.size() != 2)
		{
			return null;
		}

		return Pair.pair(values.get(0), values.get(1));
	}
}
