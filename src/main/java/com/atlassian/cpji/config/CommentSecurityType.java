package com.atlassian.cpji.config;

/**
 *
 */
public enum CommentSecurityType
{
    ROLE("cpji.project.role"), GROUP("cpji.group");

    private String labelKey;

    CommentSecurityType(String labelKey)
    {
        this.labelKey = labelKey;
    }

    public String getLabelKey()
    {
        return labelKey;
    }
}
