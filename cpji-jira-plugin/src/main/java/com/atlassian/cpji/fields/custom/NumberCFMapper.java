package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
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
    private final DoubleConverter doubleConverter;

    public NumberCFMapper(final DoubleConverter doubleConverter)
    {
        this.doubleConverter = doubleConverter;
    }

    @Override
    public String getType()
    {
        return NumberCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final Double value)
    {
        // TODO use a number formatter, or make a hex string
        return value.toString();
    }

    @Override
    protected String formatString(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        final double d = parseDouble(value);
        return doubleConverter.getString(d);
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        try
        {
            parseDouble(value);
            return true;
        }
        catch (final NumberFormatException e)
        {
            return false;
        }
    }

    private double parseDouble(final String value)
    {
        return Double.parseDouble(value);
    }
}
