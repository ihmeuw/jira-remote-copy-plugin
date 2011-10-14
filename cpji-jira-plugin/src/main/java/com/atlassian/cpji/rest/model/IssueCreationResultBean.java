package com.atlassian.cpji.rest.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "issueCreationResult")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueCreationResultBean
{
    @XmlElement (name = "issueKey")
    private String issueKey;

    @XmlElement (name = "issueId")
    private Long issueId;

    @XmlElement (name = "project")
    private String project;

    @SuppressWarnings("unused")
    public IssueCreationResultBean()
    {
    }

    public IssueCreationResultBean(String issueKey, String project, Long issueId)
    {
        this.issueKey = issueKey;
        this.project = project;
        this.issueId = issueId;
    }

    public String getIssueKey()
    {
        return issueKey;
    }

    public String getProject()
    {
        return project;
    }

    public Long getIssueId()
    {
        return issueId;
    }
}
