package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.IssueAttachmentsClient;
import com.atlassian.cpji.components.JiraLocation;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.RemotesResource;
import com.atlassian.cpji.rest.ResponseHolder;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.cpji.util.RestResponse;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
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
    public static final String REST_URL_COPY_ISSUE = "/rest/copyissue/1.0/";
    public static final String PROJECT_RESOURCE_PATH = "project";
    public static final String COPY_ISSUE_RESOURCE_PATH = "copyissue";
    public static final String CONVERT_ISSUE_LINKS_RESOURCE_PATH = COPY_ISSUE_RESOURCE_PATH + "/convertIssueLinks";

    public static final int CONNECTION_TIMEOUTS = 100000;
    private static final Logger log = Logger.getLogger(RemoteJiraProxy.class);
    private final InternalHostApplication hostApplication;
    private final ApplicationLink applicationLink;
    private final JiraLocation jiraLocation;
    private final IssueLinkClient issueLinkClient;

    public RemoteJiraProxy(InternalHostApplication hostApplication, final ApplicationLink applicationLink, JiraLocation jiraLocation, IssueLinkClient issueLinkClient) {
        this.hostApplication = hostApplication;
        this.applicationLink = applicationLink;
        this.jiraLocation = jiraLocation;
        this.issueLinkClient = issueLinkClient;
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


    @Override
    public ResponseStatus isPluginInstalled() {
        Either<ResponseStatus, Object> result = callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH, new AbstractJsonResponseHandler<ResponseStatus>(jiraLocation) {
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
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PROJECT_RESOURCE_PATH + "/issueTypeInformation/"+projectKey,
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
        return callRestService(Request.MethodType.PUT, REST_URL_COPY_ISSUE + COPY_ISSUE_RESOURCE_PATH + "/copy", new AbstractJsonResponseHandler<IssueCreationResultBean>(jiraLocation) {

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
    public Either<ResponseStatus, FieldPermissionsBean> checkPermissions(final CopyIssueBean copyIssueBean) {
        return callRestService(Request.MethodType.PUT, REST_URL_COPY_ISSUE + COPY_ISSUE_RESOURCE_PATH + "/fieldPermissions", new AbstractJsonResponseHandler<FieldPermissionsBean>(jiraLocation) {
            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.setEntity(copyIssueBean);
            }

            @Override
            protected FieldPermissionsBean parseResponse(Response response) throws ResponseException, JSONException {
                return response.getEntity(FieldPermissionsBean.class);
            }
        });
    }

    @Override
    public void copyLocalIssueLink(Issue localIssue, String copiedIssueKey, Long copiedIssueId, String localRelationship, String remoteRelationship) {
        try{
            // Create link from the copied issue
            if(remoteRelationship != null){
                final RestResponse response = issueLinkClient.createLinkFromRemoteIssue(localIssue, applicationLink, copiedIssueKey, remoteRelationship);

                if (!response.isSuccessful())
                {
                    log.error("Failed to create remote issue link from '" + copiedIssueKey + "' to '" + localIssue.getKey() + "'");
                }
            }

            // Create link from the local source issue
            if(localRelationship != null){
                issueLinkClient.createLinkToRemoteIssue(localIssue, applicationLink, copiedIssueKey, copiedIssueId, localRelationship);
            }
        } catch(Exception e){
            return;
        }

    }

    @Override
    public void copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String copiedIssueKey) {
        try{
            //todo - inline converting remote links into local?
            final RestResponse response = issueLinkClient.createRemoteIssueLink(remoteIssueLink, copiedIssueKey, applicationLink);
            if (!response.isSuccessful())
            {
                log.error("Failed to copy remote issue link. Error: Status " + response.getStatusCode() + ", Message: " + response.getStatusText());
            }
        } catch(Exception e){
            return;
        }
    }

    @Override
    public void convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {

        AbstractJsonResponseHandler<Object> handler = new AbstractJsonResponseHandler<Object>(jiraLocation) {
            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.setSoTimeout(CONNECTION_TIMEOUTS);
                request.setConnectionTimeout(CONNECTION_TIMEOUTS);
            }

            @Override
            protected Object parseResponse(Response response) throws ResponseException, JSONException {
                return new Object();
            }
        };

        callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + CONVERT_ISSUE_LINKS_RESOURCE_PATH + "/" + remoteIssueKey, handler);


    }


}
