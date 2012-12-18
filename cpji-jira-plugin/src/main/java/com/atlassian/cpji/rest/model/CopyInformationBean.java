package com.atlassian.cpji.rest.model;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v1.0
 */
@XmlRootElement(name = "copyInformation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopyInformationBean {
	@XmlElement
	private List<IssueTypeBean> issueTypes;

	@XmlElement
	private boolean attachmentsEnabled;

	@XmlElement
	private boolean hasCreateCommentPermission;

	@XmlElement
	private boolean issueLinkingEnabled;

	@XmlElement
	private boolean hasCreateAttachmentPermission;

	@XmlElement
	private UserBean user;

	@XmlElement
	private boolean hasCreateIssuePermission;

	@XmlElement
	private boolean hasCreateLinksPermission;

	@XmlElement
	private String jiraVersion;


    @XmlElement
    private Long maxAttachmentSize;

	// for testing
	public CopyInformationBean() {
	}

	public CopyInformationBean(
			final Iterable<IssueTypeBean> issueTypes,
			final boolean attachmentsEnabled,
			final boolean issueLinkingEnabled,
			final UserBean user,
			final boolean hasCreateIssuePermission,
			final boolean hasCreateAttachmentPermission,
			final boolean hasCreateCommentPermission,
			final boolean hasCreateLinksPermission,
			final String jiraVersion,
            final Long maxAttachmentSize

    ) {
		this.hasCreateCommentPermission = hasCreateCommentPermission;
		this.hasCreateLinksPermission = hasCreateLinksPermission;
		this.issueTypes = Lists.newArrayList(issueTypes);
		this.attachmentsEnabled = attachmentsEnabled;
		this.issueLinkingEnabled = issueLinkingEnabled;
		this.user = user;
		this.hasCreateIssuePermission = hasCreateIssuePermission;
		this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
		this.jiraVersion = jiraVersion;
        this.maxAttachmentSize = maxAttachmentSize;
	}

	public List<IssueTypeBean> getIssueTypes() {
		return issueTypes;
	}

	public boolean getAttachmentsEnabled() {
		return attachmentsEnabled;
	}

	public UserBean getRemoteUser() {
		return user;
	}

	public boolean getHasCreateIssuePermission() {
		return hasCreateIssuePermission;
	}

	public boolean getHasCreateAttachmentPermission() {
		return hasCreateAttachmentPermission;
	}

	public String getJiraVersion() {
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

	public boolean getHasCreateCommentPermission() {
		return hasCreateCommentPermission;
	}

	public void setHasCreateCommentPermission(boolean hasCreateCommentPermission) {
		this.hasCreateCommentPermission = hasCreateCommentPermission;
	}

	public boolean getHasCreateLinksPermission() {
		return hasCreateLinksPermission;
	}

	public void setHasCreateLinksPermission(boolean hasCreateLinksPermission) {
		this.hasCreateLinksPermission = hasCreateLinksPermission;
	}

    public Long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }
}
