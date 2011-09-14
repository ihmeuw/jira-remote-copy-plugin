package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.customfields.impl.NumberCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * Maps the {@link NumberCFType} custom field type.
 *
 * @since v2.1
 */
public class NumberCFMapper extends AbstractSingleValueCFMapper<Double>
{
    @Override
    public String getType()
    {
        return NumberCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final Double value)
    {
        return value.toString();
    }

    @Override
    protected String formatString(final String value)
    {
        // TODO use DoubleConverter?
        return value;
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        try
        {
            Double.parseDouble(value);
            return true;
        }
        catch (final NumberFormatException e)
        {
            return false;
        }
    }
}
