package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.jira.issue.fields.Field;

/**
 * @since v1.4
 */
public abstract class AbstractFieldMapper implements FieldMapper
{
    final String nameKey;
    final String id;

    public AbstractFieldMapper(Field field)
    {
        this.nameKey = field.getNameKey();
        this.id = field.getId();
    }

    public String getFieldId()
    {
        return id;
    }

    public String getFieldNameKey()
    {
        return nameKey;
    }
}
