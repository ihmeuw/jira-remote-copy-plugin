package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private Long targetParentId;

    private String priority;

    private UserBean reporter;

    private String environment;

    private UserBean assignee;

    private List<CommentBean> comments;

    private TimeTrackingBean timeTracking;

    private String issueSecurityLevel;

    private List<String> labels;

    private List<ComponentBean> components;

    private List<VersionBean> affectedVersions;

    private List<VersionBean> fixedForVersions;

    private List<UserBean> watchers;

    private List<String> visibleSystemFieldIds;

    private List<CustomFieldBean> customFields;

    private List<UserBean> voters;

	private Map<String, String[]> actionParams;

	private Map<String, Object> fieldValuesHolder;

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

    public UserBean getReporter()
    {
        return reporter;
    }

    public void setReporter(final UserBean reporter)
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

    public UserBean getAssignee()
    {
        return assignee;
    }

    public void setAssignee(final UserBean assignee)
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

    public List<UserBean> getWatchers()
    {
        return watchers;
    }

    public void setWatchers(final List<UserBean> watchers)
    {
        this.watchers = watchers;
    }

    public List<UserBean> getVoters()
    {
        return voters;
    }

    public void setVoters(final List<UserBean> voters)
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

    public Long getTargetParentId() {
        return targetParentId;
    }

    public void setTargetParentId(Long targetParentId) {
        this.targetParentId = targetParentId;
    }

	public Map<String, String[]> getActionParams() {
		return actionParams;
	}

	public void setActionParams(Map<String, String[]> actionParams) {
		this.actionParams = actionParams;
	}

	public Map<String, Object> getFieldValuesHolder() {
		return fieldValuesHolder;
	}

	public void setFieldValuesHolder(Map<String, Object> fieldValuesHolder) {
		this.fieldValuesHolder = fieldValuesHolder;
	}
}


