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
public class SelectListCFMapper implements CustomFieldMapper
{
    public SelectListCFMapper()
    {
    }

    public String getType()
    {
        return SelectCFType.class.getCanonicalName();
    }

    public CustomFieldBean createFieldBean(final CustomField customField, final Issue issue)
    {
        Object value = customField.getValue(issue);
        if (value instanceof Option)
        {
            return new CustomFieldBean(customField.getCustomFieldType().getClass().getCanonicalName(), customField.getName(), customField.getId(), Lists.newArrayList(((Option) value).getValue()));
        }
        return new CustomFieldBean(customField.getCustomFieldType().getClass().getCanonicalName(), customField.getName(), customField.getId(), Collections.<String>emptyList());
    }

    public MappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
    {
        FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        Options options = customField.getOptions("notUsed", fieldConfig, null);
        final List<String> unmappedValues = new ArrayList<String>();
        boolean hasValidValue = false;
        for (String value : customFieldBean.getValues())
        {
            Option optionForValue = options.getOptionForValue(value, null);
            if (optionForValue == null)
            {
                unmappedValues.add(value);
            }
            else
            {
                hasValidValue = true;
            }
        }
        return new MappingResult(unmappedValues, hasValidValue, false);
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
    {
        FieldConfig fieldConfig = customField.getRelevantConfig(new IssueContextImpl(project.getId(), issueType.getId()));
        Options options = customField.getOptions("notUsed", fieldConfig, null);
        final List<String> values = new ArrayList<String>();
        if (customFieldBean.getValues() != null)
        {
            for (String value : customFieldBean.getValues())
            {
                Option optionForValue = options.getOptionForValue(value, null);
                if (optionForValue != null)
                {
                    values.add(optionForValue.getValue());
                }
            }
            String[] valuesArr = new String[values.size()];
            values.toArray(valuesArr);
            inputParameters.addCustomFieldValue(customField.getId(), valuesArr);
        }
    }

}
