package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.cpji.util.DateUtil;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.customfields.converters.DatePickerConverter;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Maps the {@link DateCFMapper} custom field type.
 *
 * @since v1.4
 */
public class DateCFMapper implements CustomFieldMapper
{
    private final DatePickerConverter datePickerConverter;

    public DateCFMapper(final DatePickerConverter datePickerConverter)
    {
        this.datePickerConverter = datePickerConverter;
    }

    public String getType()
    {
        return DateCFType.class.getCanonicalName();
    }

    public CustomFieldBean createFieldBean(final CustomField customField, final Issue issue)
    {
        final Object value = customField.getValue(issue);
        final List<String> values;
        
        if (value instanceof Date)
        {
            // Convert date to string
            final String stringValue = DateUtil.toString((Date) value);
            values = Lists.newArrayList(stringValue);
        }
        else
        {
            // Value is null or unrecognised type, ignore it
            values = Collections.emptyList();
        }

        final String customFieldType = customField.getCustomFieldType().getClass().getCanonicalName();
        return new CustomFieldBean(customFieldType, customField.getName(), customField.getId(), values);
    }

    public MappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
    {
        // Because the field is not a list, there will never be unmapped values
        final List<String> unmappedValues = Collections.emptyList();

        final String value = getValue(customFieldBean);
        final boolean hasValidValue = (value != null) && DateUtil.isValidDate(value);

        return new MappingResult(unmappedValues, hasValidValue, false);
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
    {
        final String value = getValue(customFieldBean);
        if (value != null)
        {
            final Date date = DateUtil.parseString(value);
            final String formattedValue = datePickerConverter.getString(date);
            inputParameters.addCustomFieldValue(customField.getId(), formattedValue);
        }
    }

    private String getValue(final CustomFieldBean customFieldBean)
    {
        final List<String> values = customFieldBean.getValues();
        if (values == null || values.isEmpty())
        {
            return null;
        }
        
        return values.get(0);
    }
}
