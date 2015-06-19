package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.CustomFieldMappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for mapping custom fields which contain a collection of values.
 *
 * @param <T> the type of value stored by the custom field
 * @since v2.1
 */
public abstract class AbstractMultiValueCFMapper<T> extends AbstractCustomFieldMapper
{

    private static final Logger log = Logger.getLogger(AbstractMultiValueCFMapper.class);

	protected AbstractMultiValueCFMapper(DefaultFieldValuesManager defaultFieldValuesManager) {
        super(defaultFieldValuesManager);
	}

	/**
     * Convert the value stored by the custom field to a String. The value will not be null.
     *
     * @param value the value given by the custom field, see {@link com.atlassian.jira.issue.fields.CustomField#getValue(com.atlassian.jira.issue.Issue)}
     * @return a String representing the value
     */
    protected abstract String convertToString(T value);

    /**
     * Format the String value for the generated by {@link #convertToString(Object)}. The given String will have been
     * generated on the source JIRA instance. On the destination instance, we may need to format the value differently,
     * e.g. if the value is a Date, each JIRA instance may have a different format for representing dates as strings.
     *
     *
	 * @param value a String representing the custom field value
	 * @param issueType
	 * @return a formatted String recognisable by the current JIRA instance
     */
    protected abstract String formatStringForInputParams(String value, CustomField customField, Project project, IssueType issueType);

    /**
     * Determines if the given value is value for the given custom field. The value will not be null.
     *
     * @param value the custom field value, will not be null
     * @param customField
     * @param project
     * @param issueType
     * @return true if the value is valid, false if otherwise
     */
    protected abstract boolean isValidValue(String value, CustomField customField, Project project, IssueType issueType);

    @Override
    public CustomFieldBean createFieldBean(final CustomField customField, final Issue issue)
    {
        final Object value = customField.getValue(issue);
        List<String> values;

        final Collection<?> collection = castToCollection(value);
        if (collection != null)
        {
            values = new ArrayList<>(collection.size());
            for (final Object element : collection)
            {
                try
                {
                    final T t = convertToGenericType(element);
                    // If element is null or unrecognised type, ignore it
                    if (t != null)
                    {
                        final String stringValue = convertToString(t);
                        values.add(stringValue);
                    }
                }
                catch (final ClassCastException e)
                {
                    log.warn(this.getClass().getName() + " cannot cast CustomField value to specified type", e);
                }
            }
        }
        else
        {
            // Value is null or unrecognised type, ignore it
            values = Collections.emptyList();
        }

        final String customFieldType = customField.getCustomFieldType().getClass().getCanonicalName();
        return new CustomFieldBean(customFieldType, customField.getName(), customField.getId(), values);
    }

    private Collection<?> castToCollection(final Object value)
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            return (Collection) value;
        }
        catch (final ClassCastException e)
        {
            return null;
        }
    }

    protected T convertToGenericType(final Object value)
    {
        if (value == null)
        {
            return null;
        }
        return (T) value;
    }

    @Override
    public CustomFieldMappingResult getMappingResult(final CustomFieldBean customFieldBean, final CustomField customField, final Project project, final IssueType issueType)
    {
        final List<String> validValues;
        final List<String> invalidValues;

        final List<String> values = customFieldBean.getValues();
        if (values != null)
        {
            validValues = new ArrayList<>();
            invalidValues = new ArrayList<>();

            for (final String value : values)
            {
                if (isValidValue(value, customField, project, issueType))
                {
                    validValues.add(value);
                }
                else
                {
                    invalidValues.add(value);
                }
            }
        }
        else
        {
            validValues = Collections.emptyList();
            invalidValues = Collections.emptyList();
        }

        return new CustomFieldMappingResult(validValues, invalidValues, hasDefaultValueDefined(customField, project, issueType));
    }

    @Override
    public void populateInputParameters(final IssueInputParameters inputParameters, final CustomFieldMappingResult mappingResult, final CustomField customField, final Project project, final IssueType issueType)
    {
        final List<String> formattedValues = new ArrayList<>(mappingResult.getValidValues().size());
        for (final String value : mappingResult.getValidValues())
        {
            formattedValues.add(formatStringForInputParams(value, customField, project, issueType));
        }

        final String[] array = new String[formattedValues.size()];
        formattedValues.toArray(array);
        inputParameters.addCustomFieldValue(customField.getId(), array);
    }

}
