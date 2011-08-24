package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.rest.model.PermissionBean;

/**
 * @since v1.4
 */
public interface PermissionBeanCreator<T extends PermissionBean>
{
    T createPermissionBean(ValidationCode validationCode);

}
