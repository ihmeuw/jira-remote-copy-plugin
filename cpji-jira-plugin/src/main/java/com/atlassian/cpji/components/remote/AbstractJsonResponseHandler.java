package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.fugue.Either;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

/**
* @since v1.0
*/
abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<ResponseStatus, T>>
{
    private static final Logger log = Logger.getLogger(AbstractJsonResponseHandler.class);

    private final JiraLocation jiraLocation;
    private ResponseStatus providedResponseStatus = null;

    AbstractJsonResponseHandler(JiraLocation jiraLocation) {
        this.jiraLocation = jiraLocation;
    }

    public Either<ResponseStatus, T> credentialsRequired(final Response response) throws ResponseException
    {
        return Either.left(ResponseStatus.authorizationRequired(jiraLocation));
    }

    public Either<ResponseStatus, T> handle(final Response response) throws ResponseException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Response is: " + response.getResponseBodyAsString());
        }
        if (response.getStatusCode() == 401)
        {
            return Either.left(ResponseStatus.authenticationFailed(jiraLocation));
        }
        if (response.getStatusCode() == 404)
        {
            return Either.left(ResponseStatus.pluginNotInstalled(jiraLocation));
        }
        if(!response.isSuccessful()){
            return Either.left(ResponseStatus.communicationFailed(jiraLocation));
        }
        try {
            T parsedResponse = parseResponse(response);
            if(providedResponseStatus != null){
                return Either.left(providedResponseStatus);
            } else {
                return Either.right(parsedResponse);
            }
        } catch (JSONException e) {
            log.error(String.format("Failed to parse JSON from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
            return Either.left(ResponseStatus.communicationFailed(jiraLocation));
        }
    }

    protected T provideResponseStatus(ResponseStatus status){
        providedResponseStatus = status;
        return null;
    }

    protected abstract T parseResponse(Response response) throws ResponseException, JSONException;

    protected void modifyRequest(ApplicationLinkRequest request){

    }
}
