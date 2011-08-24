package com.atlassian.cpji.config;

/**
 * @since v2.0
 */
public class CommentSecurityLevel
{
    private final String id;
    private final String label;
    private final CommentSecurityType type;

    public CommentSecurityLevel(final String id, final String label, final CommentSecurityType type)
    {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public CommentSecurityType getType()
    {
        return type;
    }

    public boolean isGroupLevel()
    {
        return CommentSecurityType.GROUP.equals(type);
    }

}
