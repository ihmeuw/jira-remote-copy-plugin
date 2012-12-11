package com.atlassian.cpji.rest.model;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v1.0
 */
@XmlRootElement(name = "issueType")
public class IssueTypeBean
{
	@XmlElement
    private String name;

	@XmlElement
	private List<IssueFieldBean> requiredFields;

	// used by jersey
	@SuppressWarnings("unused")
	public IssueTypeBean() {
	}

    public IssueTypeBean(final String name, final Iterable<IssueFieldBean> requiredFields)
    {
        this.name = name;
		this.requiredFields = Lists.newArrayList(requiredFields);
    }

    public String getName()
    {
        return name;
    }

	public List<IssueFieldBean> getRequiredFields() {
		return requiredFields;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRequiredFields(List<IssueFieldBean> requiredFields) {
		this.requiredFields = Lists.newArrayList(requiredFields);
	}
}
