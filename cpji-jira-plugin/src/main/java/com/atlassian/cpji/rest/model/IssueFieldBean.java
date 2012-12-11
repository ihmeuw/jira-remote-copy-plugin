package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v3.0
 */
@XmlRootElement (name = "issueField")
public class IssueFieldBean {
	@XmlElement
	private String name;

	@XmlElement
	private String id;

	// for jersey
	@SuppressWarnings("unused")
	public IssueFieldBean() {

	}

	public IssueFieldBean(String name, String id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}
}
