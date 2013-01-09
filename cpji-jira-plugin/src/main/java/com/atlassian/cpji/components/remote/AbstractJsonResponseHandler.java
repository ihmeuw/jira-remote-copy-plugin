package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.fugue.Either;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

/**
 * @since v1.0
 */
abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<NegativeResponseStatus, T>> {
    private static final Logger log = Logger.getLogger(AbstractJsonResponseHandler.class);

    private final JiraLocation jiraLocation;
    private NegativeResponseStatus providedResponseStatus = null;

    AbstractJsonResponseHandler(JiraLocation jiraLocation) {
        this.jiraLocation = jiraLocation;
    }

    public Either<NegativeResponseStatus, T> credentialsRequired(final Response response) throws ResponseException {
        return Either.left(NegativeResponseStatus.authorizationRequired(jiraLocation));
    }

    public Either<NegativeResponseStatus, T> handle(final Response response) throws ResponseException {
        if (log.isDebugEnabled()) {
            log.debug("Response is: " + response.getResponseBodyAsString());
        }
        if (response.getStatusCode() == 401) {
            return Either.left(NegativeResponseStatus.authenticationFailed(jiraLocation));
        }
        if (response.getStatusCode() == 404) {
            return Either.left(NegativeResponseStatus.pluginNotInstalled(jiraLocation));
        }
        if (!response.isSuccessful()) {
			try {
				ErrorBean error = response.getEntity(ErrorBean.class);
				return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, error));
			} catch (ResponseException e) {
            	return Either.left(NegativeResponseStatus.communicationFailed(jiraLocation));
			}
        }
        try {
            T parsedResponse = parseResponse(response);
            if (providedResponseStatus != null) {
                return Either.left(providedResponseStatus);
            } else {
                return Either.right(parsedResponse);
            }
        } catch (JSONException e) {
            log.error(String.format("Failed to parse JSON from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
            return Either.left(NegativeResponseStatus.communicationFailed(jiraLocation));
        }
    }

    protected T provideResponseStatus(NegativeResponseStatus status) {
        providedResponseStatus = status;
        return null;
    }

    protected abstract T parseResponse(Response response) throws ResponseException, JSONException;

    protected void modifyRequest(ApplicationLinkRequest request) {

    }
}
