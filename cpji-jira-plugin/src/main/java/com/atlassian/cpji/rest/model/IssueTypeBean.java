package com.atlassian.cpji.rest.model;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

import java.util.List;

/**
 * @since v1.0
 */
@JsonRootName("issueType")
public class IssueTypeBean
{
	@JsonProperty
    private String name;

	@JsonProperty
	private List<String> requiredFields;

	// used by jersey
	@SuppressWarnings("unused")
	public IssueTypeBean() {
	}

	@JsonCreator
    public IssueTypeBean(@JsonProperty("name") final String name, @JsonProperty("requiredFields") final Iterable<String> requiredFields)
    {
        this.name = name;
		this.requiredFields = Lists.newArrayList(requiredFields);
    }

    public String getName()
    {
        return name;
    }

	public List<String> getRequiredFields() {
		return requiredFields;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRequiredFields(List<String> requiredFields) {
		this.requiredFields = Lists.newArrayList(requiredFields);
	}
}
