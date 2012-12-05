package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.model.ResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.RemotesResource;
import com.atlassian.cpji.rest.model.*;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.cpji.util.RestResponse;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFilePart;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import java.io.File;
import java.net.URI;

/**
 * @since v3.0
 */
public class RemoteJiraProxy implements JiraProxy {
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

    public RemoteJiraProxy(final InternalHostApplication hostApplication, final ApplicationLink applicationLink, final JiraLocation jiraLocation, final IssueLinkClient issueLinkClient) {
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
    public Either<ResponseStatus, Projects> getProjects() {
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
        } catch (CredentialsRequiredException ex) {
            return Either.left(ResponseStatus.authorizationRequired(jiraLocation));
        } catch (ResponseException e) {
            log.error(String.format("Failed to transform response from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
            return Either.left(ResponseStatus.communicationFailed(jiraLocation));
        }
    }


    @Override
    public Either<ResponseStatus, SuccessfulResponse> isPluginInstalled() {
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH, new AbstractJsonResponseHandler<SuccessfulResponse>(jiraLocation) {
            @Override
            protected SuccessfulResponse parseResponse(Response response) throws ResponseException, JSONException {
                if (PluginInfoResource.PLUGIN_INSTALLED.equals(response.getResponseBodyAsString().toLowerCase())) {
                    log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin installed.");
                    return SuccessfulResponse.build(jiraLocation);
                }
                log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin NOT installed.");
                return provideResponseStatus(ResponseStatus.pluginNotInstalled(jiraLocation));
            }
        });
    }

    @Override
    public String generateAuthenticationUrl(String issueId) {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        return RemotesResource.generateAuthorizationUrl(hostApplication, requestFactory, issueId);
    }

    @Override
    public Either<ResponseStatus, CopyInformationBean> getCopyInformation(String projectKey) {
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PROJECT_RESOURCE_PATH + "/issueTypeInformation/" + projectKey,
                new AbstractJsonResponseHandler<CopyInformationBean>(jiraLocation) {

                    @Override
                    protected CopyInformationBean parseResponse(Response response) throws ResponseException, JSONException {
                        return response.getEntity(CopyInformationBean.class);
                    }
                });
    }

    @Override
    public Either<ResponseStatus, IssueCreationResultBean> copyIssue(final CopyIssueBean copyIssueBean) {

        return callRestService(Request.MethodType.PUT, REST_URL_COPY_ISSUE + COPY_ISSUE_RESOURCE_PATH + "/copy", new AbstractJsonResponseHandler<IssueCreationResultBean>(jiraLocation) {

            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.setSoTimeout(CONNECTION_TIMEOUTS);
                request.setConnectionTimeout(CONNECTION_TIMEOUTS);
                request.setEntity(copyIssueBean);
            }

            @Override
            protected IssueCreationResultBean parseResponse(Response response) throws ResponseException, JSONException {
                if (response.isSuccessful()) {
                    return response.getEntity(IssueCreationResultBean.class);
                } else {
                    ErrorBean errorBean = response.getEntity(ErrorBean.class);
                    return provideResponseStatus(ResponseStatus.errorOccured(jiraLocation, errorBean));
                }

            }
        });
    }

    @Override
    public Either<ResponseStatus, SuccessfulResponse> addAttachment(final String issueKey, final File attachmentFile, final String filename, final String contentType) {
        return callRestService(Request.MethodType.POST, "rest/api/latest/issue/" + issueKey + "/attachments", new AbstractJsonResponseHandler<SuccessfulResponse>(jiraLocation) {
            @Override
            protected SuccessfulResponse parseResponse(Response response) throws ResponseException, JSONException {
                if (response.isSuccessful()) {
                    return SuccessfulResponse.build(jiraLocation);
                } else {
                    return provideResponseStatus(ResponseStatus.errorOccured(jiraLocation, response.getResponseBodyAsString()));
                }
            }

            ;

            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.addHeader("X-Atlassian-Token", "nocheck");
                RequestFilePart requestFilePart = new RequestFilePart(contentType, filename, attachmentFile, "file");
                request.setFiles(ImmutableList.of(requestFilePart));
            }
        });
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
    public Either<ResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
        try {
            // Create link from the copied issue
            String remoteRelationship = remoteDirection.getNameFromIssueLinkType(issueLinkType);
            if (remoteRelationship != null) {
                final RestResponse response = issueLinkClient.createLinkFromRemoteIssue(localIssue, applicationLink, remoteIssueKey, remoteRelationship);

                if (!response.isSuccessful()) {
                    log.error("Failed to create remote issue link from '" + remoteIssueKey + "' to '" + localIssue.getKey() + "'");
                }
            }

            // Create link from the local source issue
            String localRelationship = localDirection.getNameFromIssueLinkType(issueLinkType);
            if (localRelationship != null) {
                issueLinkClient.createLinkToRemoteIssue(localIssue, applicationLink, remoteIssueKey, remoteIssueId, localRelationship);
            }
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (Exception e) {
            return Either.left(ResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<ResponseStatus, SuccessfulResponse> copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String copiedIssueKey) {
        try {
            //todo - inline converting remote links into local?
            final RestResponse response = issueLinkClient.createRemoteIssueLink(remoteIssueLink, copiedIssueKey, applicationLink);
            if (!response.isSuccessful()) {
                log.error("Failed to copy remote issue link. Error: Status " + response.getStatusCode() + ", Message: " + response.getStatusText());
            }
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (Exception e) {
            return Either.left(ResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<ResponseStatus, SuccessfulResponse> convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {

        AbstractJsonResponseHandler<SuccessfulResponse> handler = new AbstractJsonResponseHandler<SuccessfulResponse>(jiraLocation) {
            @Override
            protected void modifyRequest(ApplicationLinkRequest request) {
                request.setSoTimeout(CONNECTION_TIMEOUTS);
                request.setConnectionTimeout(CONNECTION_TIMEOUTS);
            }

            @Override
            protected SuccessfulResponse parseResponse(Response response) throws ResponseException, JSONException {
                return SuccessfulResponse.build(jiraLocation);
            }
        };

        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + CONVERT_ISSUE_LINKS_RESOURCE_PATH + "/" + remoteIssueKey, handler);


    }

    @Override
    public String getIssueUrl(String issueKey) {
        URI displayUrl = applicationLink.getDisplayUrl();
        return displayUrl + "/browse/" + issueKey;
    }


}
