package com.atlassian.cpji.rest.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
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
	
	public static Predicate<IssueTypeBean> hasName(final String name) {
		Preconditions.checkNotNull(name);
		return new Predicate<IssueTypeBean>() {
			@Override
			public boolean apply(@Nullable IssueTypeBean input) {
				return input != null && StringUtils.equals(input.getName(), name);
			}
		};		
	}
}
