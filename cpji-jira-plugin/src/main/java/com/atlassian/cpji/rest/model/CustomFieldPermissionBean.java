package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

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

    public CustomFieldPermissionBean(final String fieldId, final String fieldName, final String validationCode, final List<String> unmappedFieldValues)
    {
        super(validationCode, unmappedFieldValues, fieldId);
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
