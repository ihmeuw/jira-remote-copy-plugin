package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v2.0
 */
@XmlRootElement (name = "userBean")
public class UserBean
{
     @XmlElement
    private String userName;

    @XmlElement
    private String email;

    @XmlElement
    private String fullName;

    @SuppressWarnings("unused")
    public UserBean(){}


    public UserBean(String userName, String email, final String fullName)
    {
        this.userName = userName;
        this.email = email;
        this.fullName = fullName;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getEmail()
    {
        return email;
    }

    public String getFullName()
    {
        return fullName;
    }
}
