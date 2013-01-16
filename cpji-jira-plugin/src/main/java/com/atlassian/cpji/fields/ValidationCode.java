package com.atlassian.cpji.fields;

/**
 */
public enum ValidationCode
{
    OK("cpji.field.validation.ok"),
    FIELD_NOT_MAPPED("cpji.field.validation.field.not.mapped"),
    FIELD_VALUE_NOT_MAPPED("cpji.field.validation.value.not.mapped"),
    FIELD_PERMISSION_MISSING("cpji.field.valiation.permission.missing"),
    FIELD_MANDATORY_NO_PERMISSION("cpji.field.validation.field.mandatory.permission.missing"),

    FIELD_MANDATORY_VALUE_NOT_MAPPED("cpji.field.validation.field.mandatory.value.not.mapped"),

    FIELD_MANDATORY_VALUE_NOT_MAPPED_USING_DEFAULT_VALUE("cpji.field.validation.field.mandatory.value.not.mapped.using.default"),

    FIELD_MANDATORY_NO_PERMISSION_MAPPED_USING_DEFAULT_VALUE("cpji.field.validation.field.mandatory.permission.missing.using.default"),

    FIELD_MANDATORY_BUT_NOT_SUPPLIED("cpji.field.validation.field.mandatory.but.not.supplied"),

    FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE("cpji.field.validation.field.mandatory.but.not.supplied.using.default"),

	FIELD_TYPE_NOT_SUPPORTED("cpji.field.validation.field.type.not.supplied");

    private final String i18nKey;

    ValidationCode(final String i18nKey)
    {
        this.i18nKey = i18nKey;
    }

    public String getI18nKey()
    {
        return i18nKey;
    }
}
