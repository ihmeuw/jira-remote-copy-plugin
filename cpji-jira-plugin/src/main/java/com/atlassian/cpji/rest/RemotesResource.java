package com.atlassian.cpji.rest;

import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.RemoteJiraService;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.model.AvailableProjectsBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import javax.inject.Inject;


/**
 *
 * @since v2.1
 */
@Path("remotes")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class RemotesResource {

	private final RemoteJiraService remoteJiraService;
    private final JiraProxyFactory jiraProxyFactory;

	@Inject
	public RemotesResource(RemoteJiraService remoteJiraService, JiraProxyFactory jiraProxyFactory) {
		this.remoteJiraService = remoteJiraService;
        this.jiraProxyFactory = jiraProxyFactory;
    }


	@Nullable
	public static String generateAuthorizationUrl(@Nonnull InternalHostApplication hostApplication, @Nonnull AuthorisationURIGenerator uriGenerator, @Nonnull String issueId)
	{
		final String url = hostApplication.getBaseUrl() + "/secure/SelectTargetProject!default.jspa?id=" + issueId;
		final URI authorisationUri = uriGenerator.getAuthorisationURI(URI.create(url));
		return authorisationUri != null ? authorisationUri.toString() : null;
	}

	@GET
	@Path("availableDestinations")
	public Response getAvailableDestinationProjects(@QueryParam("issueId") final String issueId) {
		return Response.ok(AvailableProjectsBean.create(jiraProxyFactory, issueId,
				remoteJiraService.getProjects())).build();
	}

}
