package com.atlassian.cpji.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.4
 */
@XmlRootElement (name = "customFieldPermissionBean")
public class CustomFieldPermissionBean extends PermissionBean
{
    @XmlElement (name = "fieldName")
    private String fieldName;

    @XmlElement (name = "fieldType")
    private String fieldType;

    @SuppressWarnings("unused")
    public CustomFieldPermissionBean()
    {
    }

    public CustomFieldPermissionBean(final String fieldName, final String fieldType, final String validationCode, final List<String> unmappedFieldValues)
    {
        super(validationCode, unmappedFieldValues);
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public String getFieldType()
    {
        return fieldType;
    }
}
