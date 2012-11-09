package com.atlassian.cpji.action;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.RemoteJiraService;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Comparator;
import java.util.Map;

/**
 * @since v1.2
 */
public class SelectTargetProjectAction extends AbstractCopyIssueAction
{

    public static final String AUTHORIZE = "authorize";
	private final InternalHostApplication hostApplication;
	private final RemoteJiraService remoteJiraService;
    private final WebResourceManager webResourceManager;

	private static final Logger log = Logger.getLogger(SelectTargetProjectAction.class);
    private String authorizationUrl;

	public SelectTargetProjectAction(
            final SubTaskManager subTaskManager,
            final InternalHostApplication hostApplication,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager,
			final RemoteJiraService remoteJiraService,
			final ApplicationLinkService applicationLinkService,
            final WebResourceManager webResourceManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory,
				fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager, applicationLinkService);
		this.hostApplication = hostApplication;
		this.remoteJiraService = remoteJiraService;
        this.webResourceManager = webResourceManager;
        webResourceManager.requireResource(PLUGIN_KEY + ":copyissue-js");
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
        final SelectedProject selectedEntityLink;
        try
        {
            selectedEntityLink = getSelectedDestinationProject();
        }
        catch (Exception ex)
        {
            log.error("Failed to find entity link", ex);
            addErrorMessage("Failed to find entity link! Reason: '" + targetEntityLink + ex.getMessage() + "'");
            return ERROR;
        }

		final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(selectedEntityLink.getApplicationId());
		final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();

        try
        {
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, CopyIssueToInstanceAction.REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH);
            ResponseStatus responseStatus = request.execute(new ApplicationLinkResponseHandler<ResponseStatus>()
            {
                public ResponseStatus credentialsRequired(final Response response) throws ResponseException
                {
                    return ResponseStatus.authorizationRequired(applicationLink);
                }

                public ResponseStatus handle(final Response response) throws ResponseException
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Response is: " + response.getResponseBodyAsString());
                    }
                    if (response.getStatusCode() == 401)
                    {
                        log.debug("Authentication failed to remote JIRA instance '" + applicationLink.getName() + "'");
                        return ResponseStatus.authenticationFailed(applicationLink);
                    }
                    if (PluginInfoResource.PLUGIN_INSTALLED.equals(response.getResponseBodyAsString().toLowerCase()))
                    {
                        log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin installed.");
                        return ResponseStatus.ok(applicationLink);
                    }
                    log.debug("Remote JIRA instance '" + applicationLink.getName() + "' has the CPJI plugin NOT installed.");
                    return ResponseStatus.pluginNotInstalled(applicationLink);
                }
            });
            if (ResponseStatus.Status.AUTHORIZATION_REQUIRED.equals(responseStatus.getStatus()))
            {
                return generateAuthorizationUrl(requestFactory);
            }
            else if (ResponseStatus.Status.PLUGIN_NOT_INSTALLED.equals(responseStatus.getStatus()))
            {
                log.warn("Remote JIRA instance does NOT have the CPJI plugin installed.");
                addErrorMessage("Remote JIRA instance does NOT have the CPJI plugin installed.");
                return ERROR;
            }
            else if (ResponseStatus.Status.AUTHENTICATION_FAILED.equals(responseStatus.getStatus()))
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


	public ImmutableList<Map.Entry<String, String>> getAvailableDestinationProjects() {
		final Iterable<Map.Entry<String, String>> projects = ImmutableList.of();
		for(final Projects remoteProjects : Either.allRight(remoteJiraService.getProjects())) {
			Iterables.concat(projects, Iterables.transform(remoteProjects.getResult(),
					new Function<BasicProject, Pair>() {
						@Override
						public Pair apply(@Nullable BasicProject basicProject) {
							return Pair
									.of(remoteProjects.getApplicationLink().getId().get() + "|" + basicProject.getKey(),
											String.format("(%s) %s (%s)", remoteProjects.getApplicationLink().getName(),
													basicProject.getName(),
													basicProject.getKey()));
						}
					}));
		}

		return Ordering.from(new Comparator<Map.Entry<String,String>>() {
			@Override
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		}).immutableSortedCopy(projects);
	}

    public String getAuthorizationUrl()
    {
        return authorizationUrl;
    }


}
