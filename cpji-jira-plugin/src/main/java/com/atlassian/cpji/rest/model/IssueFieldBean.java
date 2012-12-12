package com.atlassian.cpji.rest.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

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

	public static Predicate<IssueFieldBean> hasId(final Collection<String> ids) {
		Preconditions.checkNotNull(ids);
		return new Predicate<IssueFieldBean>() {
			@Override
			public boolean apply(@Nullable IssueFieldBean input) {
				return input != null && ids.contains(input.getId());
			}
		};
	}
}
