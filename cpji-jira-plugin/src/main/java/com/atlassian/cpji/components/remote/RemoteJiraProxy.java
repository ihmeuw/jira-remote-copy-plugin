package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.model.*;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.RemotesResource;
import com.atlassian.cpji.rest.model.*;
import com.atlassian.cpji.util.IssueLinkClient;
import com.atlassian.cpji.util.ResponseUtil;
import com.atlassian.cpji.util.RestResponse;
import io.atlassian.fugue.Either;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.AttachmentReadException;
import com.atlassian.jira.issue.attachment.NoAttachmentDataException;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.io.InputStreamConsumer;
import com.atlassian.modzdetector.IOUtils;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFilePart;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @since v3.0
 */
public class RemoteJiraProxy implements JiraProxy {
    public static final String REST_URL_COPY_ISSUE = "/rest/copyissue/1.0/";
    public static final String PROJECT_RESOURCE_PATH = "project";
    public static final String COPY_ISSUE_RESOURCE_PATH = "copyissue";
    public static final String CONVERT_ISSUE_LINKS_RESOURCE_PATH = COPY_ISSUE_RESOURCE_PATH + "/convertIssueLinks";
    public static final String CLEAR_ISSUE_HISTORY_RESOURCE_PATH = COPY_ISSUE_RESOURCE_PATH + "/clearIssueHistory";

    public static final int CONNECTION_TIMEOUTS = 100000;
    private static final Logger log = Logger.getLogger(RemoteJiraProxy.class);
    private final InternalHostApplication hostApplication;
    private final ApplicationLink applicationLink;
    private final JiraLocation jiraLocation;
    private final IssueLinkClient issueLinkClient;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final AttachmentManager attachmentManager;


    public RemoteJiraProxy(final InternalHostApplication hostApplication, final ApplicationLink applicationLink,
                           final JiraLocation jiraLocation, final IssueLinkClient issueLinkClient,
                           final JiraAuthenticationContext jiraAuthenticationContext, final AttachmentManager attachmentManager) {
        this.hostApplication = hostApplication;
        this.applicationLink = applicationLink;
        this.jiraLocation = jiraLocation;
        this.issueLinkClient = issueLinkClient;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentManager = attachmentManager;
    }

    @Override
    public JiraLocation getJiraLocation() {
        return jiraLocation;
    }

    @Override
    public Either<NegativeResponseStatus, Projects> getProjects() {
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PROJECT_RESOURCE_PATH, new AbstractJsonResponseHandler<Projects>(jiraLocation) {
            @Override
            protected Projects parseResponse(Response response) throws ResponseException, JSONException {
                return new Projects(jiraLocation, new BasicProjectsJsonParser().parse(
                        new JSONArray(new JSONTokener(ResponseUtil.getResponseAsTrimmedString(response)))));
            }
        });

    }


    private <T> Either<NegativeResponseStatus, T> callRestService(Request.MethodType method, final String path, final AbstractJsonResponseHandler<T> handler) {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        try {
            ApplicationLinkRequest request = requestFactory.createRequest(method, path);
            handler.modifyRequest(request);
            return request.execute(handler);
        } catch (CredentialsRequiredException ex) {
            return Either.left(NegativeResponseStatus.authorizationRequired(jiraLocation));
        } catch (ResponseException e) {
            log.error(String.format("Failed to transform response from Application Link: %s (%s)", jiraLocation.getId(), e.getMessage()));
            return Either.left(NegativeResponseStatus.communicationFailed(jiraLocation));
        }
    }


    @Override
    public Either<NegativeResponseStatus, PluginVersion> isPluginInstalled() {
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH, new AbstractJsonResponseHandler<PluginVersion>(jiraLocation) {
            @Override
            protected PluginVersion parseResponse(Response response) throws ResponseException, JSONException {
                final String version = ResponseUtil.getResponseAsTrimmedString(response).trim();
                if (version.startsWith(PluginInfoResource.PLUGIN_INSTALLED)) {
                    log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin installed.");
                    if (version.equals(PluginInfoResource.PLUGIN_INSTALLED)) {
                        return new PluginVersion(jiraLocation, PluginInfoResource.PLUGIN_INSTALLED);
                    } else {
                        String[] respArray = StringUtils.split(version, " ", 2);
                        return new PluginVersion(jiraLocation, respArray[1]);
                    }

                }
                log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin NOT installed.");
                return provideResponseStatus(NegativeResponseStatus.pluginNotInstalled(jiraLocation));
            }
        });

    }

    @Override
    public String generateAuthenticationUrl(String issueId) {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        return RemotesResource.generateAuthorizationUrl(hostApplication, requestFactory, issueId);
    }

    @Override
    public Either<NegativeResponseStatus, CopyInformationBean> getCopyInformation(String projectKey) {
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + PROJECT_RESOURCE_PATH + "/issueTypeInformation/" + projectKey,
                new AbstractJsonResponseHandler<CopyInformationBean>(jiraLocation) {

                    @Override
                    protected CopyInformationBean parseResponse(Response response) throws ResponseException, JSONException {
                        return response.getEntity(CopyInformationBean.class);
                    }
                });
    }

    @Override
    public Either<NegativeResponseStatus, IssueCreationResultBean> copyIssue(final CopyIssueBean copyIssueBean) {

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
                    return provideResponseStatus(NegativeResponseStatus.errorOccured(jiraLocation, errorBean));
                }
            }
        });
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> addAttachment(final String issueKey, final Attachment attachment) {
        try {
            final File attachmentFile = attachmentManager.streamAttachmentContent(attachment, new InputStreamConsumer<File>() {
                @Override
                public File withInputStream(final InputStream inputStream) throws IOException {
                    final File file = File.createTempFile("attachment", ".tmp");
                    final FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        IOUtils.copy(inputStream, fileOutputStream);
                    } finally {
                        fileOutputStream.close();
                    }
                    return file;
                }
            });

            try {
                return callRestService(Request.MethodType.POST, "rest/api/latest/issue/" + issueKey + "/attachments", new AbstractJsonResponseHandler<SuccessfulResponse>(jiraLocation) {
                    @Override
                    public Either<NegativeResponseStatus, SuccessfulResponse> handle(Response response) throws ResponseException {
                        //api provides 404 when attachments exceeds max size
                        if (response.getStatusCode() == 404) {
                            final String responseString = ResponseUtil.getResponseAsTrimmedString(response);
                            if (StringUtils.contains(responseString, "exceeds its maximum")) {
                                String message = jiraAuthenticationContext.getI18nHelper().getText("cpji.attachment.is.too.big");
                                return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, message));
                            }
                        }

                        return super.handle(response);
                    }

                    @Override
                    protected SuccessfulResponse parseResponse(Response response) throws ResponseException, JSONException {
                        if (response.isSuccessful()) {
                            return SuccessfulResponse.build(jiraLocation);
                        } else {
                            return provideResponseStatus(NegativeResponseStatus.errorOccured(jiraLocation, ResponseUtil.getResponseAsTrimmedString(response)));
                        }
                    }

                    @Override
                    protected void modifyRequest(ApplicationLinkRequest request) {
                        RequestFilePart requestFilePart = new RequestFilePart(attachment.getMimetype(), attachment.getFilename(), attachmentFile, "file");
                        request.setFiles(ImmutableList.of(requestFilePart));
                        request.addHeader("X-Atlassian-Token", "nocheck");
                    }
                });
            } finally {
                if (!attachmentFile.delete()) {
                    log.warn(String.format("Temporary attachment file (%s) was not successfully deleted.", attachmentFile.getAbsolutePath()));
                }
            }
        } catch (AttachmentReadException e) {
            if (e.getCause() == null) {
                return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
            } else {
                return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getCause().getMessage()));
            }
        } catch (NoAttachmentDataException e) {
            if (e.getCause() == null) {
                return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
            } else {
                return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getCause().getMessage()));
            }
        } catch (IOException e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, FieldPermissionsBean> checkPermissions(final CopyIssueBean copyIssueBean) {
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
    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, IssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
        return copyLocalIssueLink(localIssue, remoteIssueKey, remoteIssueId, new SimplifiedIssueLinkType(issueLinkType), localDirection, remoteDirection);
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalIssueLink(Issue localIssue, String remoteIssueKey, Long remoteIssueId, SimplifiedIssueLinkType issueLinkType, LinkCreationDirection localDirection, LinkCreationDirection remoteDirection) {
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
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> copyRemoteIssueLink(RemoteIssueLink remoteIssueLink, String copiedIssueKey) {
        try {
            //todo - inline converting remote links into local?
            final RestResponse response = issueLinkClient.createRemoteIssueLink(remoteIssueLink, copiedIssueKey, applicationLink);
            if (!response.isSuccessful()) {
                log.error("Failed to copy remote issue link. Error: Status " + response.getStatusCode() + ", Message: " + response.getStatusText());
            }
            return SuccessfulResponse.buildEither(jiraLocation);
        } catch (Exception e) {
            return Either.left(NegativeResponseStatus.errorOccured(jiraLocation, e.getMessage()));
        }
    }

    @Override
    public Either<NegativeResponseStatus, SuccessfulResponse> convertRemoteIssueLinksIntoLocal(String remoteIssueKey) {

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
    public Either<NegativeResponseStatus, SuccessfulResponse> clearChangeHistory(String issueKey) {
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
        return callRestService(Request.MethodType.GET, REST_URL_COPY_ISSUE + CLEAR_ISSUE_HISTORY_RESOURCE_PATH + "/" + issueKey, handler);
    }

    @Override
    public String getIssueUrl(String issueKey) {
        URI displayUrl = applicationLink.getDisplayUrl();
        return displayUrl + "/browse/" + issueKey;
    }


}
