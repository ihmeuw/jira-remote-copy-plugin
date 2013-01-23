package com.atlassian.cpji.action;

import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.rest.model.PermissionBean;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
* TODO: Document this class / interface here
*
* @since v3.0
*/
public class MissingFieldPermissionDescription
{
	private final ValidationCode validationCode;
	private final PermissionBean permissionBean;
	private final String fieldName;
	private final String validationMessage;
	private final boolean canCopyIssue;

	public MissingFieldPermissionDescription(PermissionBean permissionBean, String name, String validationMessage, final boolean canCopyIssue)
	{
		this.permissionBean = permissionBean;
		this.fieldName = name;
		this.validationMessage =  validationMessage;
		this.canCopyIssue = canCopyIssue;
		this.validationCode = ValidationCode.valueOf(permissionBean.getValidationCode());
	}

	public String getFieldName()
	{
		return fieldName;
	}

	public String getValidationMessage()
	{
		return validationMessage;
	}

	public boolean canCopyIssue()
	{
		return canCopyIssue;
	}

	public String getFieldId() {
		return permissionBean.getFieldId();
	}

	public ValidationCode getValidationCode() {
		return this.validationCode;
	}

	public static Predicate<MissingFieldPermissionDescription> isDestinationFieldRequired() {
		return new Predicate<MissingFieldPermissionDescription>() {
			@Override
			public boolean apply(MissingFieldPermissionDescription input) {
				Preconditions.checkNotNull(input);
				switch(input.getValidationCode()) {
					case FIELD_MANDATORY_NO_PERMISSION:
					case FIELD_MANDATORY_VALUE_NOT_MAPPED:
					case FIELD_MANDATORY_VALUE_NOT_MAPPED_USING_DEFAULT_VALUE:
					case FIELD_MANDATORY_NO_PERMISSION_MAPPED_USING_DEFAULT_VALUE:
					case FIELD_MANDATORY_BUT_NOT_SUPPLIED:
					case FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE:
						return true;
					default:
						return false;

				}
			}
		};
	}

}
