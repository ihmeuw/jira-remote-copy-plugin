package com.atlassian.cpji.action;

import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.rest.model.PermissionBean;

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
}
