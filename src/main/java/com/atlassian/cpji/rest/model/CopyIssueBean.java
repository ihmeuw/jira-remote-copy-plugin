package com.atlassian.cpji.rest.model;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @since v1.0
 */
@XmlRootElement (name = "copyIssue")
public class CopyIssueBean
{
    private String summary;

    private String description;

    private String originalKey;

    private String baseUrl;

    private String targetProjectKey;

    private String targetIssueType;

    private String priority;

    private String reporter;

    private String environment;

    private String assignee;

    private List<CommentBean> comments;

    private TimeTrackingBean timeTracking;

    private String issueSecurityLevel;

    private List<String> labels;

    private List<ComponentBean> components;

    private List<VersionBean> affectedVersions;

    private List<VersionBean> fixedForVersions;

    private List<String> watchers;

    private List<String> visibleSystemFieldIds;

    private List<CustomFieldBean> customFields;

    private List<String> voters;

    @XmlJavaTypeAdapter (DateAdapter.class)
    private Date dueDate;

    @SuppressWarnings("unused")
    public CopyIssueBean()
    {
    }

    public List<CommentBean> getComments()
    {
        return comments;
    }

    public void setComments(final List<CommentBean> comments)
    {
        this.comments = comments;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getOriginalKey()
    {
        return originalKey;
    }

    public void setOriginalKey(String originalKey)
    {
        this.originalKey = originalKey;
    }

    public String getTargetProjectKey()
    {
        return targetProjectKey;
    }

    public void setTargetProjectKey(String targetProjectKey)
    {
        this.targetProjectKey = targetProjectKey;
    }

    public String getTargetIssueType()
    {
        return targetIssueType;
    }

    public void setTargetIssueType(String targetIssueType)
    {
        this.targetIssueType = targetIssueType;
    }

    public void setPriority(final String priority)
    {
        this.priority = priority;
    }

    public String getPriority()
    {
        return priority;
    }

    public String getReporter()
    {
        return reporter;
    }

    public void setReporter(final String reporter)
    {
        this.reporter = reporter;
    }

    public void setEnvironment(final String environment)
    {
        this.environment = environment;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public String getAssignee()
    {
        return assignee;
    }

    public void setAssignee(final String assignee)
    {
        this.assignee = assignee;
    }

    public void setIssueDueDate(final Date dueDate)
    {
        this.dueDate = dueDate;
    }

    public Date getIssueDueDate()
    {
        return dueDate;
    }

    public void setTimeTracking(TimeTrackingBean timeTracking)
    {
        this.timeTracking = timeTracking;
    }

    public TimeTrackingBean getTimeTracking()
    {
        return timeTracking;
    }

    public String getIssueSecurityLevel()
    {
        return issueSecurityLevel;
    }

    public void setIssueSecurityLevel(final String issueSecurityLevel)
    {
        this.issueSecurityLevel = issueSecurityLevel;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public void setLabels(final List<String> labels)
    {
        this.labels = labels;
    }

    public List<ComponentBean> getComponents()
    {
        return components;
    }

    public void setComponents(final List<ComponentBean> components)
    {
        this.components = components;
    }

    public List<VersionBean> getAffectedVersions()
    {
        return affectedVersions;
    }

    public void setAffectedVersions(final List<VersionBean> affectedVersions)
    {
        this.affectedVersions = affectedVersions;
    }

    public List<VersionBean> getFixedForVersions()
    {
        return fixedForVersions;
    }

    public void setFixedForVersions(final List<VersionBean> fixedForVersions)
    {
        this.fixedForVersions = fixedForVersions;
    }

    public List<String> getWatchers()
    {
        return watchers;
    }

    public void setWatchers(final List<String> watchers)
    {
        this.watchers = watchers;
    }

    public List<String> getVoters()
    {
        return voters;
    }

    public void setVoters(final List<String> voters)
    {
        this.voters = voters;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(final String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public List<String> getVisibleSystemFieldIds()
    {
        return visibleSystemFieldIds;
    }

    public void setVisibleSystemFieldIds(final List<String> visibleSystemFieldIds)
    {
        this.visibleSystemFieldIds = visibleSystemFieldIds;
    }

    public void setCustomFields(final List<CustomFieldBean> customFields)
    {
        this.customFields = customFields;
    }

    public List<CustomFieldBean> getCustomFields()
    {
        return customFields;
    }
}


