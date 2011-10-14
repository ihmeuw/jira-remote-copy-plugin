package com.atlassian.cpji.util;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.UrlBuilder;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.json.JSONTokener;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ReturningResponseHandler;

import java.util.Iterator;

/**
 * Helper class for making JIRA remote issue links REST calls.
 */
public class RemoteIssueLinkClient
{
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String REST_BASE_URL = "rest/api/2/issue";
    private static final String REMOTE_LINK_RESOURCE = "remotelink";

    public RestResponse createRemoteIssueLink(final InternalHostApplication internalHostApplication,  final Issue issue, final ApplicationLink applicationLink, final String remoteIssueKey, final String relationship) throws CredentialsRequiredException, ResponseException
    {
        final ApplicationLinkRequest request = createCreateRemoteIssueLinkRequest(applicationLink, remoteIssueKey);
        request.setRequestContentType(JSON_CONTENT_TYPE);
        request.setRequestBody(getJsonForCreateRemoteIssueLink(internalHostApplication, issue, relationship));
        return request.executeAndReturn(new RestResponseHandler());
    }

    private String getJsonForCreateRemoteIssueLink(final InternalHostApplication internalHostApplication, final Issue issue, final String relationship)
    {
        try
        {
            final JSONObject json = new JSONObject();

            final String globalId = encodeGlobalId(internalHostApplication, issue);
            json.put("globalId", globalId);

            final JSONObject application = new JSONObject();
            application.put("type", "com.atlassian.jira");
            application.put("name", internalHostApplication.getName());
            json.put("application", application);

            json.put("relationship", relationship);

            // Only store the bare minimum information, the rest will be shown using the renderer plugin
            final JSONObject object = new JSONObject();
            object.put("url", buildIssueUrl(internalHostApplication.getBaseUrl().toASCIIString(), issue.getKey()));
            object.put("title", issue.getKey());
            json.put("object", object);

            return json.toString();
        }
        catch (final JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String buildIssueUrl(final String baseUri, final String issueKey)
    {
        return new UrlBuilder(baseUri)
                .addPathUnsafe("browse")
                // TODO use addPath() when we go to a more recent version of JIRA
                .addPathUnsafe(issueKey)
                .asUrlString();
    }

    private static ApplicationLinkRequest createCreateRemoteIssueLinkRequest(final ApplicationLink applicationLink, final String issueKey) throws CredentialsRequiredException
    {
        final UrlBuilder urlBuilder = new UrlBuilder(REST_BASE_URL)
                .addPathUnsafe(REST_BASE_URL)
                // TODO use addPath() when we go to a more recent version of JIRA
                .addPathUnsafe(issueKey)
                .addPathUnsafe(REMOTE_LINK_RESOURCE);
        final String restUrl = urlBuilder.asUrlString();
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        return requestFactory.createRequest(Request.MethodType.POST, restUrl);
    }

    private String encodeGlobalId(final InternalHostApplication internalHostApplication, final Issue issue)
    {
        return "appId=" + internalHostApplication.getId() + "&issueId=" + issue.getId();
    }

    private static ErrorCollection convertJsonToErrorCollection(final JSONObject json)
    {
        final ErrorCollection errors = new SimpleErrorCollection();

        try
        {
            final JSONArray errorMessages = json.getJSONArray("errorMessages");
            for (int i = 0; i < errorMessages.length(); i++)
            {
                errors.addErrorMessage(errorMessages.getString(i));
            }

            final JSONObject errorsMap = json.getJSONObject("errors");
            final Iterator<String> keys = errorsMap.keys();
            while (keys.hasNext())
            {
                final String key = keys.next();
                errors.addError(key, errorsMap.getString(key));
            }
        }
        catch (final JSONException e)
        {
            return null;
        }

        return errors;
    }

    private static class RestResponseHandler implements ReturningResponseHandler<Response, RestResponse>
    {
        @Override
        public RestResponse handle(final Response response) throws ResponseException
        {
            ErrorCollection errors = null;
            if (!response.isSuccessful())
            {
                // Check if the response contains an ErrorCollection
                try
                {
                    final String responseString = response.getResponseBodyAsString();
                    final JSONObject json = new JSONObject(new JSONTokener(responseString));
                    errors = convertJsonToErrorCollection(json);
                }
                catch (final JSONException e)
                {
                    // Response did not contain an ErrorCollection
                    errors = null;
                }
            }

            return new RestResponse(errors, response.getStatusCode(), response.getStatusText(), response.isSuccessful());
        }
    }
}
