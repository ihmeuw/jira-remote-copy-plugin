package com.atlassian.cpji.rest.model;

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

	@SuppressWarnings("unused")
    public CustomFieldPermissionBean()
    {
    }

    public CustomFieldPermissionBean(final String fieldId, final String fieldName, final String validationCode)
    {
        super(validationCode, fieldId);
		this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

}
