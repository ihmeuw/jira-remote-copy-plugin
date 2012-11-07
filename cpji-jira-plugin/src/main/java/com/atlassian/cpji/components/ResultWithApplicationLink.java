package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;

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

}
