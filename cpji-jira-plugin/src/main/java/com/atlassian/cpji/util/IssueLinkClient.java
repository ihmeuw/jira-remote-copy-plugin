package com.atlassian.cpji.util;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.jira.bc.issue.issuelink.RemoteIssueLinkService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
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
import org.apache.log4j.Logger;

import java.util.Iterator;

/**
 * Component for create (remote) issue links.
 */
public class IssueLinkClient
{
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String REST_BASE_URL = "rest/api/2/issue";
    private static final String REMOTE_LINK_RESOURCE = "remotelink";
    private final InternalHostApplication internalHostApplication;
    private final RemoteIssueLinkService remoteIssueLinkService;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    private static final Logger log = Logger.getLogger(IssueLinkClient.class);

    public IssueLinkClient(InternalHostApplication internalHostApplication, final JiraAuthenticationContext jiraAuthenticationContext, final RemoteIssueLinkService remoteIssueLinkService)
    {
        this.internalHostApplication = internalHostApplication;
        this.remoteIssueLinkService = remoteIssueLinkService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public RestResponse createRemoteIssueLinkFromIssue(final Issue issue, final ApplicationLink applicationLink, final String remoteIssueKey, final String relationship)
            throws CredentialsRequiredException, ResponseException
    {
        final ApplicationLinkRequest request = createCreateRemoteIssueLinkRequest(applicationLink, remoteIssueKey);
        request.setRequestContentType(JSON_CONTENT_TYPE);
        request.setRequestBody(getJsonForCreateRemoteIssueLink(internalHostApplication, issue, relationship));
        return request.executeAndReturn(new RestResponseHandler());
    }

    public void createRemoteLinkToIssue(final Issue sourceIssue, final ApplicationLink applicationLink, final String targetIssueKey, final Long targetIssueId)
    {
        RemoteIssueLinkBuilder remoteIssueLinkBuilder = new RemoteIssueLinkBuilder();
        String globalId = encodeGlobalId(applicationLink.getId(), targetIssueId);
        remoteIssueLinkBuilder.globalId(globalId);
        remoteIssueLinkBuilder.applicationType("com.atlassian.jira");
        remoteIssueLinkBuilder.relationship("copied to");
        String url = buildIssueUrl(applicationLink.getDisplayUrl().toASCIIString(), targetIssueKey);
        remoteIssueLinkBuilder.url(url);
        remoteIssueLinkBuilder.applicationName(applicationLink.getName());
        remoteIssueLinkBuilder.issueId(sourceIssue.getId());
        remoteIssueLinkBuilder.title(targetIssueKey);

        RemoteIssueLinkService.CreateValidationResult issueLinkValidationResult = remoteIssueLinkService.validateCreate(jiraAuthenticationContext.getLoggedInUser(), remoteIssueLinkBuilder.build());
        if (issueLinkValidationResult.isValid())
        {
            RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkResult = remoteIssueLinkService.create(jiraAuthenticationContext.getLoggedInUser(), issueLinkValidationResult);
        }
        else
        {
           log.error("Failed to create issue link to remote JIRA issue with key '" + targetIssueKey + "' Error(s): " + issueLinkValidationResult.getErrorCollection());
        }
    }

    private String getJsonForCreateRemoteIssueLink(final InternalHostApplication internalHostApplication, final Issue issue, final String relationship)
    {
        try
        {
            final JSONObject json = new JSONObject();

            final String globalId = encodeGlobalId(internalHostApplication.getId(), issue.getId());
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

    private static ApplicationLinkRequest createCreateRemoteIssueLinkRequest(final ApplicationLink applicationLink, final String issueKey)
            throws CredentialsRequiredException
    {
        final UrlBuilder urlBuilder = new UrlBuilder(REST_BASE_URL)
                        // TODO use addPath() when we go to a more recent version of JIRA
                .addPathUnsafe(issueKey)
                .addPathUnsafe(REMOTE_LINK_RESOURCE);
        final String restUrl = urlBuilder.asUrlString();
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        return requestFactory.createRequest(Request.MethodType.POST, restUrl);
    }

    private String encodeGlobalId(final ApplicationId appId, final Long issueId)
    {
        return "appId=" + appId.get() + "&issueId=" + issueId;
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
