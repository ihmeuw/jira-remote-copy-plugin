package com.atlassian.cpji.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement(name = "issueType")
public class IssueTypeBean
{
    @XmlElement (name = "types")
    private List<String> getTypes;

    @SuppressWarnings("unused")
    public IssueTypeBean()
    {
    }

    public IssueTypeBean(final List<String> getTypes)
    {
        this.getTypes = getTypes;
    }

    public List<String> getGetTypes()
    {
        return getTypes;
    }
}
