package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.fields.MappingConstants;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManagerImpl;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.PermissionBean;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public abstract class AbstractFieldMappingChecker<T extends PermissionBean> implements MappingChecker<T>
{
    private final DefaultFieldValuesManagerImpl defaultFieldValuesManager;
    protected final CopyIssueBean copyIssueBean;
    protected final Project project;
    protected final FieldLayout fieldLayout;

    protected AbstractFieldMappingChecker(final DefaultFieldValuesManagerImpl defaultFieldValuesManager, final CopyIssueBean copyIssueBean, final Project project, FieldLayout fieldLayout)
    {
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.copyIssueBean = copyIssueBean;
        this.project = project;
        this.fieldLayout = fieldLayout;
    }

    protected T checkField(MappingResult mappingResult, String fieldId, boolean hasPermission, PermissionBeanCreator<T> permissionBeanCreator)
    {
        if (hasPermission && !mappingResult.hasOneValidValue())
        {
            if (isFieldRequired(fieldLayout, fieldId))
            {
                if (mappingResult.hasDefault())
                {
                    return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_MANDATORY_VALUE_NOT_MAPPED_USING_DEFAULT_VALUE);
                }
                return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_MANDATORY_VALUE_NOT_MAPPED);
            }
            else if (mappingResult.isEmpty())
            {
                return null;
            }
            else
            {
                return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_VALUE_NOT_MAPPED);
            }
        }
        else if (!hasPermission)
        {
            if (isFieldRequired(fieldLayout, fieldId))
            {
                if (mappingResult.hasDefault())
                {
                    return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_MANDATORY_NO_PERMISSION_MAPPED_USING_DEFAULT_VALUE);
                }
                return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_MANDATORY_NO_PERMISSION);
            }
            else if (mappingResult.isEmpty())
            {
                return null;
            }
            else
            {
				if (mappingResult.hasDefault()) {
					return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_PERMISSION_MISSING_USING_DEFAULT_VALUE);
				} else {
                	return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_PERMISSION_MISSING);
				}
            }
        }
        else if (hasPermission && !mappingResult.getUnmappedValues().isEmpty())
        {
            return permissionBeanCreator.createPermissionBean(ValidationCode.FIELD_VALUE_NOT_MAPPED);
        }
        else
        {
            return permissionBeanCreator.createPermissionBean(ValidationCode.OK);
        }
    }

    protected boolean hasDefaultValue(final String fieldId)
    {
        return defaultFieldValuesManager.hasDefaultValue(project.getKey(), fieldId, copyIssueBean.getTargetIssueType());
    }

    protected boolean isFieldRequired(FieldLayout fieldLayout, String fieldId)
    {
        if (MappingConstants.nonSystemFieldsFieldIds.contains(fieldId))
        {
            return false;
        }
        return fieldLayout.getFieldLayoutItem(fieldId).isRequired();
    }

}
