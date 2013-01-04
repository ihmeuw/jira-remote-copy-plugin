package com.atlassian.cpji.action;

/**
* TODO: Document this class / interface here
*
* @since v3.0
*/
public class FieldPermission
{
	private final String fieldId;
	private final String fieldName;
	private final String validationMessage;
	private final boolean canCopyIssue;

	public FieldPermission(String fieldId, String name, String validationMessage, final boolean canCopyIssue)
	{
		this.fieldId = fieldId;
		this.fieldName = name;
		this.validationMessage =  validationMessage;
		this.canCopyIssue = canCopyIssue;
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
		return fieldId;
	}
}
