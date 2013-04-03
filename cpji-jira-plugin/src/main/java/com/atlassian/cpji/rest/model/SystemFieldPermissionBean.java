package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v1.0
 */
@XmlRootElement (name = "fieldPermission")
public class SystemFieldPermissionBean extends PermissionBean
{
    @SuppressWarnings("unused")
    public SystemFieldPermissionBean()
    {
    }

    public SystemFieldPermissionBean(final String fieldId, final String validationCode, final List<String> unmappedValues)
    {
        super(validationCode, fieldId, unmappedValues);
    }
}
