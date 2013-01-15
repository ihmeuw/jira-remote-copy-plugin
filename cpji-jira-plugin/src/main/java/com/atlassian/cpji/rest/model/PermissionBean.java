package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

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

    @XmlElement (name = "unmappedFieldValues")
    private List<String> unmappedFieldValues;


    public PermissionBean()
    {
    }

    public PermissionBean(final String validationCode, final List<String> unmappedFieldValues, final String fieldId)
    {
        this.validationCode = validationCode;
        this.unmappedFieldValues = unmappedFieldValues;
		this.fieldId = fieldId;
	}

    public String getValidationCode()
    {
        return validationCode;
    }

    public List<String> getUnmappedFieldValues()
    {
        return unmappedFieldValues;
    }

	public String getFieldId() {
		return fieldId;
	}
}
