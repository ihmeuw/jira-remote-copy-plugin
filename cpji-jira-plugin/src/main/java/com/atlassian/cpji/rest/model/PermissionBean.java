package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.4
 */
@XmlRootElement (name = "permissionBean")
public class PermissionBean
{
	@XmlElement (name = "fieldId")
	protected String fieldId;

	@XmlElement (name = "validationCode")
    private String validationCode;

	public PermissionBean()
    {
    }

    public PermissionBean(final String validationCode, final String fieldId)
    {
        this.validationCode = validationCode;
		this.fieldId = fieldId;
	}

    public String getValidationCode()
    {
        return validationCode;
    }

	public String getFieldId() {
		return fieldId;
	}
}
