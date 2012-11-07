package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.jira.rest.client.domain.BasicProject;

/**
* @since v2.1
*/
public class Projects extends ResultWithApplicationLink<Iterable<BasicProject>> {
	public Projects(ApplicationLink applicationLink, Iterable<BasicProject> result) {
		super(applicationLink, result);
	}
}
