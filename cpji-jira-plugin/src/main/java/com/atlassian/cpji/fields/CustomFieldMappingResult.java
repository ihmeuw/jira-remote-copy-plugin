package com.atlassian.cpji.fields;

import java.util.List;

/**
 * @since v1.4
 */
public class CustomFieldMappingResult extends MappingResult
{
    private final List<String> validValues;

    public CustomFieldMappingResult(final List<String> validValues, final List<String> invalidValues)
    {
        super(invalidValues, !validValues.isEmpty(), validValues.isEmpty() && invalidValues.isEmpty());
        this.validValues = validValues;
    }

    public List<String> getValidValues()
    {
        return validValues;
    }
}