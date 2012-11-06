package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;

/**
* TODO: Document this class / interface here
*
* @since v5.2
*/
public class ResponseStatus
{
	private final ApplicationLink applicationLink;
	private final Status status;

	public ResponseStatus(ApplicationLink applicationLink, Status status) {
		this.applicationLink = applicationLink;
		this.status = status;
	}

	public static ResponseStatus communicationFailed(ApplicationLink applicationLink) {
		return new ResponseStatus(applicationLink, Status.COMMUNICATION_FAILED);
	}

	public static ResponseStatus authenticationFailed(ApplicationLink applicationLink) {
		return new ResponseStatus(applicationLink, Status.AUTHENTICATION_FAILED);
	}

	public static ResponseStatus ok(ApplicationLink applicationLink) {
		return new ResponseStatus(applicationLink, Status.OK);
	}

	public static ResponseStatus pluginNotInstalled(ApplicationLink applicationLink) {
		return new ResponseStatus(applicationLink, Status.PLUGIN_NOT_INSTALLED);
	}

	public enum Status {
			COMMUNICATION_FAILED,
			AUTHORIZATION_REQUIRED,
			AUTHENTICATION_FAILED,
			PLUGIN_NOT_INSTALLED,
			OK
	}

	public ApplicationLink getApplicationLink() {
		return applicationLink;
	}

	public Status getStatus() {
		return status;
	}

	public static ResponseStatus authorizationRequired(ApplicationLink applicationLink) {
		return new ResponseStatus(applicationLink, Status.AUTHORIZATION_REQUIRED);
	}
}
