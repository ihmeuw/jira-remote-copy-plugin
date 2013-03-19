package com.atlassian.cpji.fields;

import java.util.List;

/**
 * @since v1.4
 */
public class MappingResult
{
    private final List<String> unmappedValues;
    private final boolean hasOneValidValue;
    private final boolean isEmpty;
	private final boolean hasDefault;

	public MappingResult(final List<String> unmappedValues, final boolean hasOneValidValue, final boolean isEmpty, final boolean hasDefault)
    {
        this.unmappedValues = unmappedValues;
        this.hasOneValidValue = hasOneValidValue;
        this.isEmpty = isEmpty;
		this.hasDefault = hasDefault;
	}

    public List<String> getUnmappedValues()
    {
        return unmappedValues;
    }

    public boolean hasOneValidValue()
    {
        return hasOneValidValue;
    }

    public boolean isEmpty()
    {
        return isEmpty;
    }

	public boolean hasDefault() {
		return hasDefault;
	}
}
