package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @since v1.1
 */
@XmlRootElement (name="version")
public class VersionBean
{
    @XmlElement
    private String name;

    @SuppressWarnings("unused")
    public VersionBean() {}

    public VersionBean(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
