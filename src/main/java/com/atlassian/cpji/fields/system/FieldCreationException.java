package com.atlassian.cpji.fields.system;

/**
 *
 * @since v2.0
 */
public class FieldCreationException extends Exception
{
    private final String fieldId;

    public FieldCreationException(final String message, final String fieldId)
    {
        super(message);
        this.fieldId = fieldId;
    }

    public FieldCreationException(final String message, final Throwable throwable, final String fieldId)
    {
        super(message, throwable);
        this.fieldId = fieldId;
    }

    public FieldCreationException(final Throwable throwable, final String fieldId)
    {
        super(throwable);
        this.fieldId = fieldId;
    }

    public String getFieldId()
    {
        return fieldId;
    }
}
