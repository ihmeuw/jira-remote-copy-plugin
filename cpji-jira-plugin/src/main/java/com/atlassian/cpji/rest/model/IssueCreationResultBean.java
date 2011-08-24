package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "issueCreationResult")
public class IssueCreationResultBean
{
    @XmlElement (name = "issueKey")
    private String issueKey;

    @XmlElement (name = "project")
    private String project;

    @SuppressWarnings("unused")
    public IssueCreationResultBean()
    {
    }

    public IssueCreationResultBean(String issueKey, String project)
    {
        this.issueKey = issueKey;
        this.project = project;
    }

    public String getIssueKey()
    {
        return issueKey;
    }

    public String getProject()
    {
        return project;
    }
}
