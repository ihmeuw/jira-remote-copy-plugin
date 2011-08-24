package com.atlassian.cpji.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "fieldPermission")
public class SystemFieldPermissionBean extends PermissionBean
{
    @XmlElement (name = "fieldId")
    private String fieldId;

    @SuppressWarnings("unused")
    public SystemFieldPermissionBean()
    {
    }

    public SystemFieldPermissionBean(final String fieldId, final String validationCode, final List<String> unmappedFieldValues)
    {
        super(validationCode, unmappedFieldValues);
        this.fieldId = fieldId;
    }

    public String getFieldId()
    {
        return fieldId;
    }



}
