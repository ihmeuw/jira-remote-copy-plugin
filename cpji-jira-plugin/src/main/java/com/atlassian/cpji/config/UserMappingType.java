package com.atlassian.cpji.config;

/**
 * @since v2.0
 */
public enum UserMappingType
{
    BY_USERNAME("usermapping.username"), BY_E_MAIL("usermapping.email"), BY_EMAIL_AND_USERNAME("usermapping.email.and.username");

    private String i18nKey;

    UserMappingType(String i18nKey)
    {
        this.i18nKey = i18nKey;
    }

    public String getI18nKey()
    {
        return i18nKey;
    }
}
