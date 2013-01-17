package com.atlassian.cpji.components.model;

import com.atlassian.jira.issue.link.IssueLinkType;

/**
 * Represents an IssueLinkType in JIRA.
 *
 * This is a simplification of {@link com.atlassian.jira.issue.link.IssueLinkType}
 */
public class SimplifiedIssueLinkType
{
    private final String inward, outward;
    private final Long id;

    public SimplifiedIssueLinkType(String outward, String inward)
    {
        this.outward = outward;
        this.inward = inward;
        this.id = null;
    }

    public SimplifiedIssueLinkType(IssueLinkType issueLinkType)
    {
        this.outward = issueLinkType.getOutward();
        this.inward = issueLinkType.getInward();
        this.id = issueLinkType.getId();
    }

    /**
     * @return the outward name of this IssueLinkType
     */
    public String getOutward()
    {
        return outward;
    }

    /**
     * @return the inward name of this IssueLinkType
     */
    public String getInward()
    {
        return inward;
    }

    public Long getId()
    {
        if (id == null)
            throw new IllegalStateException("This should not be called on this instance");
        return id;
    }
}

