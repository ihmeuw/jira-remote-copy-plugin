package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    protected String formatString(final String value)
    {
        return value;
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        final FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        final Options options = customField.getOptions("notUsed", fieldConfig, null);

        Option optionForValue = options.getOptionForValue(value, null);
        return (optionForValue != null);
    }
}
