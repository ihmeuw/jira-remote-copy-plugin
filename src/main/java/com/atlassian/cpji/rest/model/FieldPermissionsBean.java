package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v1.0
 */
@XmlRootElement (name = "fieldPermissions")
public class FieldPermissionsBean
{
    @XmlElement (name = "systemFieldPermissionBeans")
    List<SystemFieldPermissionBean> systemFieldPermissionBeans;

    @XmlElement (name = "customFieldPermissionBeans")
    List<CustomFieldPermissionBean> customFieldPermissionBeans;

    @SuppressWarnings("unused")
    public FieldPermissionsBean()
    {
    }

    public FieldPermissionsBean(final List<SystemFieldPermissionBean> systemFieldPermissionBeans, final List<CustomFieldPermissionBean> customFieldPermissionBeans)
    {
        this.systemFieldPermissionBeans = systemFieldPermissionBeans;
        this.customFieldPermissionBeans = customFieldPermissionBeans;
    }

    public List<SystemFieldPermissionBean> getSystemFieldPermissionBeans()
    {
        return systemFieldPermissionBeans;
    }

    public List<CustomFieldPermissionBean> getCustomFieldPermissionBeans()
    {
        return customFieldPermissionBeans;
    }
}
