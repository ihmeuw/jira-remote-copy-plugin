package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.1
 */
@XmlRootElement (name="component")
public class ComponentBean
{
    @XmlElement
    String name;

    @SuppressWarnings("unused")
    public ComponentBean(){}

    public ComponentBean(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
