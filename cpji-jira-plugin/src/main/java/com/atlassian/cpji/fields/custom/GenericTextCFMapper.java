package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.impl.StringCFType;
import com.atlassian.jira.issue.customfields.impl.TextAreaCFType;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * Maps the {@link TextAreaCFType} custom field type.
 *
 * @since v2.1
 */
public class GenericTextCFMapper extends AbstractSingleValueCFMapper<String>
{
    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof StringCFType	|| type instanceof GenericTextCFType || type instanceof TextCFType;
    }

    @Override
    protected String convertToString(String value)
    {
        return value;
    }

    @Override
    protected String formatString(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return value;
    }

    @Override
    protected boolean isValidValue(String value, CustomField customField, Project project, IssueType issueType)
    {
        return true;
    }
}
