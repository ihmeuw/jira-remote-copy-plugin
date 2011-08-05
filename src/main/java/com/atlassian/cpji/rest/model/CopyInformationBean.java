package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "copyInformation")
public class CopyInformationBean
{
    @XmlElement (name = "issueTypes")
    private IssueTypeBean issueTypes;

    @XmlElement( name = "attachmentDisabled")
    private Boolean attachmentsEnabled;

    @XmlElement( name = "hasCreateAttachmentPermission")
    private Boolean hasCreateAttachmentPermission;

    @XmlElement( name = "user")
    private UserBean user;

    @XmlElement( name = "hasCreateIssuePermission")
    private Boolean hasCreateIssuePermission;

    @SuppressWarnings("unused")
    public CopyInformationBean()
    {
    }

    public CopyInformationBean
            (
                    final IssueTypeBean issueTypes,
                    final Boolean attachmentsEnabled,
                    final UserBean user,
                    final Boolean hasCreateIssuePermission,
                    final Boolean hasCreateAttachmentPermission)
    {
        this.issueTypes = issueTypes;
        this.attachmentsEnabled = attachmentsEnabled;
        this.user = user;
        this.hasCreateIssuePermission = hasCreateIssuePermission;
        this.hasCreateAttachmentPermission = hasCreateAttachmentPermission;
    }

    public IssueTypeBean getIssueTypes()
    {
        return issueTypes;
    }

    public Boolean getAttachmentsEnabled()
    {
        return attachmentsEnabled;
    }

    public UserBean getRemoteUser()
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

}
