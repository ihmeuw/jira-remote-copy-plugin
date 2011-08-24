package com.atlassian.cpji.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.4
 */
@XmlRootElement (name = "permissionBean")
public class PermissionBean
{
    @XmlElement (name = "validationCode")
    private String validationCode;

    @XmlElement (name = "unmappedFieldValues")
    private List<String> unmappedFieldValues;


    public PermissionBean()
    {
    }

    public PermissionBean(final String validationCode, final List<String> unmappedFieldValues)
    {
        this.validationCode = validationCode;
        this.unmappedFieldValues = unmappedFieldValues;
    }

    public String getValidationCode()
    {
        return validationCode;
    }

    public List<String> getUnmappedFieldValues()
    {
        return unmappedFieldValues;
    }
}
