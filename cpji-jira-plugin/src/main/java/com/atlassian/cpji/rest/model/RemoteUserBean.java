package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.3
 */
@XmlRootElement (name = "remoteUserBean")
public class RemoteUserBean
{
    @XmlElement
    private String userName;

    @XmlElement
    private String fullName;

    @SuppressWarnings("unused")
    public RemoteUserBean(){}


    public RemoteUserBean(String userName, String fullName)
    {
        this.userName = userName;
        this.fullName = fullName;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getFullName()
    {
        return fullName;
    }
}
