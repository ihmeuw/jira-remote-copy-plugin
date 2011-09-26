package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public class SelectListCFMapper extends AbstractSingleValueCFMapper<Option>
{
    @Override
    public String getType()
    {
        return SelectCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final Option value)
    {
        return value.getValue();
    }

    @Override
    protected String formatString(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
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
