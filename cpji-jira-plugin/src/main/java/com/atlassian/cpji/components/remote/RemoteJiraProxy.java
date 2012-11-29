package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.IssueAttachmentsClient;
import com.atlassian.cpji.action.CopyDetailsAction;
import com.atlassian.cpji.action.CopyIssueToInstanceAction;
import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.RemotesResource;
import com.atlassian.cpji.rest.ResponseHolder;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.util.RestResponse;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import java.util.Collection;

/**
 *
 * @since v3.0
 */
public class RemoteJiraProxy implements JiraProxy
{
    public static final int CONNECTION_TIMEOUTS = 100000;
    private static final Logger log = Logger.getLogger(RemoteJiraProxy.class);
    private final InternalHostApplication hostApplication;
    private final ApplicationLink applicationLink;
    private final JiraLocation jiraLocation;

    public RemoteJiraProxy(InternalHostApplication hostApplication, final ApplicationLink applicationLink, JiraLocation jiraLocation) {
        this.hostApplication = hostApplication;
        this.applicationLink = applicationLink;
        this.jiraLocation = jiraLocation;
    }

    @Override
    public JiraLocation getJiraLocation() {
        return jiraLocation;
    }

    @Override
    public Either<ResponseStatus, Projects> getProjects()
    {
        return callRestService(Request.MethodType.GET, "/rest/copyissue/1.0/project", new AbstractJsonResponseHandler<Projects>(jiraLocation) {
            @Override
            protected Projects parseResponse(Response response) throws ResponseException, JSONException {
                return new Projects(jiraLocation, new BasicProjectsJsonParser().parse(
                        new JSONArray(new JSONTokener(response.getResponseBodyAsString()))));
            }
        });

    }


    private <T> Either<ResponseStatus, T> callRestService(Request.MethodType method, final String path, final AbstractJsonResponseHandler handler) {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        try {
            ApplicationLinkRequest request = requestFactory.createRequest(method, path);
            handler.modifyRequest(request);
            return (Either<ResponseStatus, T>) request.execute(handler);
        }
        catch (CredentialsRequiredException ex)
        {
            return Either.left(ResponseStatus.authorizationRequired(jiraLocation));
        } catch (ResponseException e) {
            log.error(String.format("Failed to transform response from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
            return Either.left(ResponseStatus.communicationFailed(jiraLocation));
        }
    }


    protected static abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<ResponseStatus, T>>
    {
        private final JiraLocation jiraLocation;

        protected AbstractJsonResponseHandler(JiraLocation jiraLocation) {
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
                return Either.right(parseResponse(response));
            } catch (JSONException e) {
                log.error(String.format("Failed to parse JSON from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
                return Either.left(ResponseStatus.communicationFailed(jiraLocation));
            }
        }

        protected abstract T parseResponse(Response response) throws ResponseException, JSONException;
        protected void modifyRequest(ApplicationLinkRequest request){

        }
    }


    @Override
    public ResponseStatus isPluginInstalled() {
        Either<ResponseStatus, Object> result = callRestService(Request.MethodType.GET, CopyIssueToInstanceAction.REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH, new AbstractJsonResponseHandler<ResponseStatus>(jiraLocation) {
            @Override
            protected ResponseStatus parseResponse(Response response) throws ResponseException, JSONException {
                if (PluginInfoResource.PLUGIN_INSTALLED.equals(response.getResponseBodyAsString().toLowerCase())) {
                    log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin installed.");
                    return ResponseStatus.ok(jiraLocation);
                }
                log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin NOT installed.");
                return ResponseStatus.pluginNotInstalled(jiraLocation);
            }
        });

        if(result.isLeft()){
            return (ResponseStatus) result.left().get();
        } else {
            return (ResponseStatus) result.right().get();
        }

    }

    @Override
    public String generateAuthenticationUrl(String issueId) {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        return RemotesResource.generateAuthorizationUrl(hostApplication, requestFactory, issueId);
    }

    @Override
    public Either<ResponseStatus, CopyInformationBean> getCopyInformation(String projectKey) {
        return callRestService(Request.MethodType.GET,
                CopyDetailsAction.REST_URL_COPY_ISSUE + CopyDetailsAction.PROJECT_RESOURCE_PATH + "/issueTypeInformation/"+projectKey,
                new AbstractJsonResponseHandler<CopyInformationBean>(jiraLocation){

            @Override
            protected CopyInformationBean parseResponse(Response response) throws ResponseException, JSONException {
                return response.getEntity(CopyInformationBean.class);
            }
        });
    }

    @Override
    public Either<ResponseStatus, IssueCreationResultBean> copyIssue(final CopyIssueBean copyIssueBean) {

        //TODO: retrieving errors on unsuccessful run
        //ErrorBean errorBean = response.getEntity(ErrorBean.class);
        return callRestService(Request.MethodType.PUT, CopyDetailsAction.REST_URL_COPY_ISSUE + CopyDetailsAction.COPY_ISSUE_RESOURCE_PATH + "/copy", new AbstractJsonResponseHandler<IssueCreationResultBean>(jiraLocation) {

            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.setSoTimeout(CONNECTION_TIMEOUTS);
                request.setConnectionTimeout(CONNECTION_TIMEOUTS);
                request.setEntity(copyIssueBean);
            }

            @Override
            protected IssueCreationResultBean parseResponse(Response response) throws ResponseException, JSONException {
                return response.getEntity(IssueCreationResultBean.class);
            }
        });
    }

    @Override
    public ErrorCollection copyAttachments(String issueKey, Collection<Attachment> attachments) {
        ErrorCollection ec = new SimpleErrorCollection();
        IssueAttachmentsClient issueAttachmentsClient = new IssueAttachmentsClient(applicationLink);
        ResponseHolder copyAttachmentResponse = issueAttachmentsClient.addAttachment(issueKey, attachments);
        if (!copyAttachmentResponse.isSuccessful())
        {
            ec.addErrorMessage("Failed to copy attachments. " + copyAttachmentResponse.getResponse());
        }
        return ec;
    }

    @Override
    public void copyIssueLinks(Long localIssueId, Long remoteIssueId, String remoteIssueKey) {
        copyLocalIssueLinks(issueToCopy, copiedIssueKey, copiedIssue.getIssueId());
        copyRemoteIssueLinks(issueToCopy, copiedIssueKey, appLink);
        convertIssueLinks(copiedIssueKey, authenticatedRequestFactory);
    }

    private void copyLocalIssueLinks(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) throws ResponseException, CredentialsRequiredException
    {
        for (final IssueLink inwardLink : issueLinkManager.getInwardLinks(localIssue.getId()))
        {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId, type.getOutward(), type.getInward());
        }

        for (final IssueLink outwardLink : issueLinkManager.getOutwardLinks(localIssue.getId()))
        {
            final IssueLinkType type = outwardLink.getIssueLinkType();
            copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type.getInward(), type.getOutward());
        }
    }

    private void copyLocalIssueLink(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId, final String localRelationship, final String remoteRelationship) throws ResponseException, CredentialsRequiredException
    {
        // Create link from the copied issue
        final RestResponse response = issueLinkClient.createLinkFromRemoteIssue(
                localIssue, applicationLink, copiedIssueKey, remoteRelationship);

        if (!response.isSuccessful())
        {
            log.error("Failed to create remote issue link from '" + copiedIssueKey + "' to '" + localIssue.getKey() + "'");
        }

        // Create link from the local source issue
        issueLinkClient.createLinkToRemoteIssue(localIssue, applicationLink, copiedIssueKey, copiedIssueId, localRelationship);
    }

    private void copyRemoteIssueLinks(final Issue localIssue, final String copiedIssueKey, final ApplicationLink appLink) throws ResponseException, CredentialsRequiredException
    {
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue))
        {
            final RestResponse response = issueLinkClient.createRemoteIssueLink(remoteIssueLink, copiedIssueKey, appLink);
            if (!response.isSuccessful())
            {
                log.error("Failed to copy remote issue link. Error: Status " + response.getStatusCode() + ", Message: " + response.getStatusText());
            }
        }
    }

    private void convertIssueLinks(final String copiedIssueKey, final ApplicationLinkRequestFactory authenticatedRequestFactory) throws CredentialsRequiredException
    {
        ApplicationLinkRequest request = authenticatedRequestFactory.createRequest(Request.MethodType.GET, REST_URL_COPY_ISSUE + CONVERT_ISSUE_LINKS_RESOURCE_PATH + "/" + copiedIssueKey);
        request.setSoTimeout(CONNECTION_TIMEOUTS);
        request.setConnectionTimeout(CONNECTION_TIMEOUTS);
        try
        {
            request.execute();
        }
        catch (final ResponseException e)
        {
            log.error("Failed to convert remote links into local links on the target server", e);
        }
    }

}
