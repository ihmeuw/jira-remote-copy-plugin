package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.jira.rest.client.domain.BasicProject;

/**
*
* @since v2.1
*/
public class ResultWithApplicationLink<T> {
	private final ApplicationLink applicationLink;
	private final T result;

	public ResultWithApplicationLink(ApplicationLink applicationLink, T result) {
		this.applicationLink = applicationLink;
		this.result = result;
	}

	public ApplicationLink getApplicationLink() {
		return applicationLink;
	}

	public T getResult() {
		return result;
	}

	public static class ResponseStatusResult extends ResultWithApplicationLink<ResponseStatus> {
		public ResponseStatusResult(ApplicationLink applicationLink, ResponseStatus result) {
			super(applicationLink, result);
		}
	}

	public static class Projects extends ResultWithApplicationLink<Iterable<BasicProject>> {
		public Projects(ApplicationLink applicationLink, Iterable<BasicProject> result) {
			super(applicationLink, result);
		}
	}

	public static class ApplicationLinkRequestResult extends ResultWithApplicationLink<ApplicationLinkRequest> {
		public ApplicationLinkRequestResult(ApplicationLink applicationLink, ApplicationLinkRequest result) {
			super(applicationLink, result);
		}
	}
}
