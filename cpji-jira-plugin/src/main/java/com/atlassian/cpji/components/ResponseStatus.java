package com.atlassian.cpji.components;

import com.atlassian.cpji.components.remote.LocalJiraProxy;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* TODO: Document this class / interface here
*
* @since v5.2
*/
public class ResponseStatus extends ResultWithJiraLocation<ResponseStatus.Status>
{
    public ResponseStatus(final JiraLocation jiraLocation, final Status result)
    {
        super(jiraLocation, result);
    }


    public static ResponseStatus communicationFailed(JiraLocation jiraLocation) {
		return new ResponseStatus(jiraLocation, Status.COMMUNICATION_FAILED);
	}

	public static ResponseStatus authenticationFailed(JiraLocation jiraLocation) {
		return new ResponseStatus(jiraLocation, Status.AUTHENTICATION_FAILED);
	}

	public static ResponseStatus ok(JiraLocation jiraLocation) {
		return new ResponseStatus(jiraLocation, Status.OK);
	}

	public static ResponseStatus pluginNotInstalled(JiraLocation jiraLocation) {
		return new ResponseStatus(jiraLocation, Status.PLUGIN_NOT_INSTALLED);
	}

	public enum Status {
			COMMUNICATION_FAILED,
			AUTHORIZATION_REQUIRED,
			AUTHENTICATION_FAILED,
			PLUGIN_NOT_INSTALLED,
			OK
	}

	public static ResponseStatus authorizationRequired(JiraLocation jiraLocation) {
		return new ResponseStatus(jiraLocation, Status.AUTHORIZATION_REQUIRED);
	}

	@Nonnull
	public static Predicate<ResponseStatus> onlyRemoteJiras() {
		return new Predicate<ResponseStatus>() {
			@Override
			public boolean apply(@Nullable ResponseStatus input) {
				return input.getJiraLocation() != null && !input.getJiraLocation().equals(LocalJiraProxy.LOCAL_JIRA_LOCATION);
			}
		};
	}
}
