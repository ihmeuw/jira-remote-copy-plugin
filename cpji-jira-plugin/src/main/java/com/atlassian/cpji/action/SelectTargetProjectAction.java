package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.EntityLink;
import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.applinks.api.application.jira.JiraProjectEntityType;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.List;

/**
 * @since v1.2
 */
public class SelectTargetProjectAction extends AbstractCopyIssueAction
{

    public static final String AUTHORIZE = "authorize";
    private final InternalHostApplication hostApplication;

    private static final Logger log = Logger.getLogger(SelectTargetProjectAction.class);
    private String authorizationUrl;

    public enum ResponseStatus
    {
        AUTHORIZATION_REQUIRED,
        AUTHENTICATION_FAILED,
        PLUGIN_NOT_INSTALLED,
        OK
    }

    public SelectTargetProjectAction(
            final SubTaskManager subTaskManager,
            final EntityLinkService entityLinkService,
            final InternalHostApplication hostApplication,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager)
    {
        super(subTaskManager, entityLinkService, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory, fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager);
        this.hostApplication = hostApplication;
    }

    @Override
    public String doDefault() throws Exception
    {
        return checkPermissions();
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        final EntityLink selectedEntityLink;
        try
        {
            selectedEntityLink = getSelectedEntityLink();
        }
        catch (Exception ex)
        {
            log.error("Failed to find entity link", ex);
            addErrorMessage("Failed to find entity link! Reason: '" + targetEntityLink + ex.getMessage() + "'");
            return ERROR;
        }

        final ApplicationLinkRequestFactory requestFactory = selectedEntityLink.getApplicationLink().createAuthenticatedRequestFactory();

        try
        {
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, CopyIssueToInstanceAction.REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH);
            ResponseStatus responseStatus = request.execute(new ApplicationLinkResponseHandler<ResponseStatus>()
            {
                public ResponseStatus credentialsRequired(final Response response) throws ResponseException
                {
                    return ResponseStatus.AUTHORIZATION_REQUIRED;
                }

                public ResponseStatus handle(final Response response) throws ResponseException
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Response is: " + response.getResponseBodyAsString());
                    }
                    if (response.getStatusCode() == 401)
                    {
                        log.debug("Authentication failed to remote JIRA instance '" + selectedEntityLink.getApplicationLink().getName() + "'");
                        return ResponseStatus.AUTHENTICATION_FAILED;
                    }
                    if ("installed".equals(response.getResponseBodyAsString().toLowerCase()))
                    {
                        log.debug("Remote JIRA instance '" + selectedEntityLink.getApplicationLink().getName() + "' has the CPJI plugin installed.");
                        return ResponseStatus.OK;
                    }
                    log.debug("Remote JIRA instance '" + selectedEntityLink.getApplicationLink().getName() + "' has the CPJI plugin NOT installed.");
                    return ResponseStatus.PLUGIN_NOT_INSTALLED;
                }
            });
            if (ResponseStatus.AUTHORIZATION_REQUIRED.equals(responseStatus))
            {
                return generateAuthorizationUrl(requestFactory);
            }
            else if (ResponseStatus.PLUGIN_NOT_INSTALLED.equals(responseStatus))
            {
                log.warn("Remote JIRA instance does NOT have the CPJI plugin installed.");
                addErrorMessage("Remote JIRA instance does NOT have the CPJI plugin installed.");
                return ERROR;
            }
            else if (ResponseStatus.AUTHENTICATION_FAILED.equals(responseStatus))
            {
                addErrorMessage("Authentication failed. Check the authentication configuration.");
                return ERROR;
            }
        }
        catch (CredentialsRequiredException ex)
        {
            return generateAuthorizationUrl(requestFactory);
        }
        return getRedirect("/secure/CopyDetailsAction.jspa?id=" + getId() + "&targetEntityLink=" + targetEntityLink);
    }

    private String generateAuthorizationUrl(AuthorisationURIGenerator uriGenerator) throws Exception
    {
        String url = hostApplication.getBaseUrl() + "/secure/SelectTargetProjectAction!default.jspa?id=" + getId() + "&targetEntityLink=" + targetEntityLink;
        authorizationUrl = uriGenerator.getAuthorisationURI(URI.create(url)).toString();
        return AUTHORIZE;
    }

    @SuppressWarnings ("unused")
    public List<EntityLink> getLinkedJiraProjects()
    {
        return Lists.newArrayList(getEntityLinks());
    }

    private Iterable<EntityLink> getEntityLinks()
    {
        return entityLinkService.getEntityLinks(getIssueObject().getProjectObject(), JiraProjectEntityType.class);
    }

    public String getAuthorizationUrl()
    {
        return authorizationUrl;
    }


}
