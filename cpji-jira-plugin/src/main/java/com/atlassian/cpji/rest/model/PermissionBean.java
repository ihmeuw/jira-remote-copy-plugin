package com.atlassian.cpji.rest.model;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
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

    @XmlElement (name = "unmappedValues")
    private List<String> unmappedValues;

	public PermissionBean()
    {
    }

    public PermissionBean(final String validationCode, final String fieldId, final List<String> unmappedValues)
    {
		Preconditions.checkNotNull(validationCode);
		Preconditions.checkNotNull(fieldId);
        this.validationCode = validationCode;
		this.fieldId = fieldId;
        this.unmappedValues = unmappedValues;
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

    public List<String> getUnmappedValues() {
        return unmappedValues;
    }
}
