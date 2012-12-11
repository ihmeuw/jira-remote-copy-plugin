package com.atlassian.cpji.rest.model;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v1.0
 */
@XmlRootElement (name = "copyInformation")
@JsonIgnoreProperties (ignoreUnknown = true)
public class CopyInformationBean
{
    @XmlElement (name = "issueTypes")
    private List<IssueTypeBean> issueTypes;

    @XmlElement( name = "attachmentsEnabled")
    private boolean attachmentsEnabled;

	@XmlElement( name = "issueLinkingEnabled")
	private boolean issueLinkingEnabled;

    @XmlElement( name = "hasCreateAttachmentPermission")
    private boolean hasCreateAttachmentPermission;

    @XmlElement( name = "user")
    private UserBean user;

    @XmlElement( name = "hasCreateIssuePermission")
    private boolean hasCreateIssuePermission;

    @XmlElement (name = "jiraVersion")
    private String jiraVersion;

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
                    final String jiraVersion)
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
}
