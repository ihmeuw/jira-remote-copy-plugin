package com.atlassian.cpji.rest.model;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @since v1.0
 */
@XmlRootElement(name="comment")
public class CommentBean
{
    @XmlElement
    private String body;

    @XmlElement
    private UserBean author;

    @XmlElement
    private String roleLevel;

    @XmlElement
    private String groupLevel;

    @XmlJavaTypeAdapter (DateAdapter.class)
    private Date created;

    @XmlJavaTypeAdapter (DateAdapter.class)
    private Date updated;

    @SuppressWarnings("unused")
    public CommentBean() {}

    public CommentBean(final String body, final UserBean author, final String roleLevel, final String groupLevel, final Date created, final Date updated)
    {
        this.body = body;
        this.author = author;
        this.roleLevel = roleLevel;
        this.groupLevel = groupLevel;
        this.created = created;
        this.updated = updated;
    }

    public String getBody()
    {
        return body;
    }

    public UserBean getAuthor()
    {
        return author;
    }

    public String getRoleLevel()
    {
        return roleLevel;
    }

    public String getGroupLevel()
    {
        return groupLevel;
    }

    public Date getCreated()
    {
        return created;
    }

    public Date getUpdated()
    {
        return updated;
    }
}
