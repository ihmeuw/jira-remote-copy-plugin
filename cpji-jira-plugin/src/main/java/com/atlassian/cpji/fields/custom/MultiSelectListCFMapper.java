package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public class MultiSelectListCFMapper extends AbstractMultiValueCFMapper<Option>
{
    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof MultiSelectCFType;
    }

    @Override
    protected String convertToString(final Option value)
    {
        return value.getValue();
    }

	@Override
	protected String formatStringForInputParams(String value, CustomField customField, Project project, IssueType issueType) {
		final Option option = getOption(value, customField, project, issueType);
		return (option == null) ? null : option.getOptionId().toString();
	}

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        final Option option = getOption(value, customField, project, issueType);
        return (option != null);
    }

    private Option getOption(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        final FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        final Options options = customField.getOptions("notUsed", fieldConfig, null);
        return options.getOptionForValue(value, null);
    }

}
