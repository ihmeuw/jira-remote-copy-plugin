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

    @XmlElement( name = "attachmentDisabled")
    private Boolean attachmentsEnabled;

    @XmlElement( name = "hasCreateAttachmentPermission")
    private Boolean hasCreateAttachmentPermission;

    @XmlElement( name = "user")
    private RemoteUserBean user;

    @XmlElement( name = "hasCreateIssuePermission")
    private Boolean hasCreateIssuePermission;

    @XmlElement (name = "version")
    private String version;

    @SuppressWarnings("unused")
    public CopyInformationBean()
    {
    }

    public CopyInformationBean
            (
                    final IssueTypeBean issueTypes,
                    final Boolean attachmentsEnabled,
                    final RemoteUserBean user,
                    final Boolean hasCreateIssuePermission,
                    final Boolean hasCreateAttachmentPermission,
                    final String version)
    {
        this.issueTypes = issueTypes;
        this.attachmentsEnabled = attachmentsEnabled;
        this.user = user;
        this.hasCreateIssuePermission = hasCreateIssuePermission;
        this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
        this.version = version;
    }

    public IssueTypeBean getIssueTypes()
    {
        return issueTypes;
    }

    public Boolean getAttachmentsEnabled()
    {
        return attachmentsEnabled;
    }

    public RemoteUserBean getRemoteUser()
    {
        return user;
    }

    public Boolean getHasCreateIssuePermission()
    {
        return hasCreateIssuePermission;
    }

    public Boolean getHasCreateAttachmentPermission()
    {
        return hasCreateAttachmentPermission;
    }

    public String getVersion()
    {
        return version;
    }
}
