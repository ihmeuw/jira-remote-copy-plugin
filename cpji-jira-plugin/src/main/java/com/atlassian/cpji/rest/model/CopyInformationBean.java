package com.atlassian.cpji.rest.model;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

import java.util.List;

/**
 * @since v1.0
 */
@JsonRootName("copyInformation")
@JsonIgnoreProperties (ignoreUnknown = true)
public class CopyInformationBean
{
	@JsonProperty
    private List<IssueTypeBean> issueTypes;

	@JsonProperty
    private boolean attachmentsEnabled;

	@JsonProperty
	private boolean issueLinkingEnabled;

	@JsonProperty
    private boolean hasCreateAttachmentPermission;

	@JsonProperty
    private UserBean user;

	@JsonProperty
    private boolean hasCreateIssuePermission;

	@JsonProperty
    private String jiraVersion;

	// for testing
	public CopyInformationBean() {
	}

	@JsonCreator
    public CopyInformationBean(
                    @JsonProperty("issueTypes") final Iterable<IssueTypeBean> issueTypes,
                    @JsonProperty("attachmentsEnabled") final boolean attachmentsEnabled,
					@JsonProperty("issueLinkingEnabled") final boolean issueLinkingEnabled,
                    @JsonProperty("user") final UserBean user,
                    @JsonProperty("hasCreateIssuePermission") final boolean hasCreateIssuePermission,
                    @JsonProperty("hasCreateAttachmentPermission") final boolean hasCreateAttachmentPermission,
                    @JsonProperty("jiraVersion") final String jiraVersion)
    {
        this.issueTypes = Lists.newArrayList(issueTypes);
        this.attachmentsEnabled = attachmentsEnabled;
		this.issueLinkingEnabled = issueLinkingEnabled;
        this.user = user;
        this.hasCreateIssuePermission = hasCreateIssuePermission;
        this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
        this.jiraVersion = jiraVersion;
    }

    public List<IssueTypeBean> getIssueTypes()
    {
        return issueTypes;
    }

    public boolean getAttachmentsEnabled()
    {
        return attachmentsEnabled;
    }

    public UserBean getRemoteUser()
    {
        return user;
    }

    public boolean getHasCreateIssuePermission()
    {
        return hasCreateIssuePermission;
    }

    public boolean getHasCreateAttachmentPermission()
    {
        return hasCreateAttachmentPermission;
    }

    public String getJiraVersion()
    {
        return jiraVersion;
    }

	public boolean getIssueLinkingEnabled() {
		return issueLinkingEnabled;
	}

	public void setIssueTypes(List<IssueTypeBean> issueTypes) {
		this.issueTypes = Lists.newArrayList(issueTypes);
	}

	public void setAttachmentsEnabled(boolean attachmentsEnabled) {
		this.attachmentsEnabled = attachmentsEnabled;
	}

	public void setIssueLinkingEnabled(boolean issueLinkingEnabled) {
		this.issueLinkingEnabled = issueLinkingEnabled;
	}

	public void setHasCreateAttachmentPermission(boolean hasCreateAttachmentPermission) {
		this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
	}

	public void setUser(UserBean user) {
		this.user = user;
	}

	public void setHasCreateIssuePermission(boolean hasCreateIssuePermission) {
		this.hasCreateIssuePermission = hasCreateIssuePermission;
	}

	public void setJiraVersion(String jiraVersion) {
		this.jiraVersion = jiraVersion;
	}
}
