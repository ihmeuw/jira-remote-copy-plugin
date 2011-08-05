package com.atlassian.cpji.fields;

/**
 */
public enum ValidationCode
{
    OK(true, "cpji.field.validation.ok"),
    FIELD_NOT_MAPPED(true, "cpji.field.validation.field.not.mapped"),
    FIELD_VALUE_NOT_MAPPED(true, "cpji.field.validation.value.not.mapped"),
    FIELD_PERMISSION_MISSING(true, "cpji.field.valiation.permission.missing"),
    FIELD_MANDATORY_NO_PERMISSION(false, "cpji.field.validation.field.mandatory.permission.missing"),

    FIELD_MANDATORY_VALUE_NOT_MAPPED(false, "cpji.field.validation.field.mandatory.value.not.mapped"),

    FIELD_MANDATORY_VALUE_NOT_MAPPED_USING_DEFAULT_VALUE(true, "cpji.field.validation.field.mandatory.value.not.mapped.using.default"),

    FIELD_MANDATORY_NO_PERMISSION_MAPPED_USING_DEFAULT_VALUE(true, "cpji.field.validation.field.mandatory.permission.missing.using.default"),

    FIELD_MANDATORY_BUT_NOT_SUPPLIED(false, "cpji.field.validation.field.mandatory.but.not.supplied"),

    FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE(true, "cpji.field.validation.field.mandatory.but.not.supplied.using.default");

    private final boolean canCopyIssue;

    private final String i18nKey;

    ValidationCode(boolean canCopyIssue, final String i18nKey)
    {
        this.canCopyIssue = canCopyIssue;
        this.i18nKey = i18nKey;
    }

    public boolean canCopyIssue()
    {
        return canCopyIssue;
    }

    public String getI18nKey()
    {
        return i18nKey;
    }
}
