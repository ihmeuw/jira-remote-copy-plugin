package com.atlassian.cpji.rest.model;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
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
		Preconditions.checkNotNull(validationCode);
		Preconditions.checkNotNull(fieldId);
        this.validationCode = validationCode;
		this.fieldId = fieldId;
	}

	@Nonnull
    public String getValidationCode()
    {
        return validationCode;
    }

	@Nonnull
	public String getFieldId() {
		return fieldId;
	}
}
