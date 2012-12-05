package com.atlassian.cpji.rest.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "copyInformation")
@JsonIgnoreProperties (ignoreUnknown = true)
public class CopyInformationBean
{
    @XmlElement (name = "issueTypes")
    private IssueTypeBean issueTypes;

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

    @XmlElement (name = "version")
    private String version;

    @SuppressWarnings("unused")
    public CopyInformationBean()
    {
    }

    public CopyInformationBean
            (
                    final IssueTypeBean issueTypes,
                    final boolean attachmentsEnabled,
					final boolean issueLinkingEnabled,
                    final UserBean user,
                    final boolean hasCreateIssuePermission,
                    final boolean hasCreateAttachmentPermission,
                    final String version)
    {
        this.issueTypes = issueTypes;
        this.attachmentsEnabled = attachmentsEnabled;
		this.issueLinkingEnabled = issueLinkingEnabled;
        this.user = user;
        this.hasCreateIssuePermission = hasCreateIssuePermission;
        this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
        this.version = version;
    }

    public IssueTypeBean getIssueTypes()
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

    public String getVersion()
    {
        return version;
    }

	public boolean getIssueLinkingEnabled() {
		return issueLinkingEnabled;
	}
}
