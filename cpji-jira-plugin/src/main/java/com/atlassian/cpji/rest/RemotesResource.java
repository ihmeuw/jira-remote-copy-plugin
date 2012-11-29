package com.atlassian.cpji.rest;

import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.RemoteJiraService;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.model.AvailableProjectsBean;
import com.atlassian.cpji.rest.model.RemotePluginBean;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 * @since v2.1
 */
@Path("remotes")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class RemotesResource {

	private final RemoteJiraService remoteJiraService;
	private final InternalHostApplication hostApplication;

	public RemotesResource(RemoteJiraService remoteJiraService, InternalHostApplication hostApplication) {
		this.remoteJiraService = remoteJiraService;
		this.hostApplication = hostApplication;
	}

	@GET
	@Path("plugins")
	public Response getRemotePlugins(@QueryParam("issueId") final String issueId) {
		return Response.ok(Iterables
				.transform(Iterables.filter(remoteJiraService.getPluginInfo(), ResponseStatus.onlyRemoteJiras()),
						new Function<ResponseStatus, RemotePluginBean>() {
							@Override
							public RemotePluginBean apply(@Nullable ResponseStatus input) {
								return RemotePluginBean.create(input, hostApplication, issueId);
							}
						})).build();
	}

	@Nullable
	public static String generateAuthorizationUrl(@Nonnull InternalHostApplication hostApplication, @Nonnull AuthorisationURIGenerator uriGenerator, @Nonnull String issueId)
	{
		final String url = hostApplication.getBaseUrl() + "/secure/SelectTargetProjectAction!default.jspa?id=" + issueId;
		final URI authorisationUri = uriGenerator.getAuthorisationURI(URI.create(url));
		return authorisationUri != null ? authorisationUri.toString() : null;
	}

	@GET
	@Path("availableDestinations")
	public Response getAvailableDestinationProjects(@QueryParam("issueId") final String issueId) {
		return Response.ok(AvailableProjectsBean.create(hostApplication, issueId,
				remoteJiraService.getProjects())).build();
	}

}
