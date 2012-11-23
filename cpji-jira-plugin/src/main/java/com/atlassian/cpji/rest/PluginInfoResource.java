package com.atlassian.cpji.rest;

import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.RemoteJiraService;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.model.RemotePluginBean;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

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
 * @since v1.1
 */
@Path (PluginInfoResource.RESOURCE_PATH)
@Consumes ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class PluginInfoResource
{
    public static final String RESOURCE_PATH = "plugininfo";
    public static final String PLUGIN_INSTALLED = "installed";

	private final RemoteJiraService remoteJiraService;
	private final InternalHostApplication hostApplication;

	public PluginInfoResource(RemoteJiraService remoteJiraService, InternalHostApplication hostApplication)
    {
		this.remoteJiraService = remoteJiraService;
		this.hostApplication = hostApplication;
	}

    @GET
    @AnonymousAllowed
    public Response pluginInfo()
    {
        return Response.ok(PLUGIN_INSTALLED).build();
    }

	@GET
	@Path("remotePlugins")
	@Produces ({MediaType.APPLICATION_JSON})
	public Response getRemotePlugins(@QueryParam("issueId") final String issueId) {
		return Response.ok(Iterables
				.transform(Iterables.filter(remoteJiraService.getPluginInfo(), ResponseStatus.hasApplicationLink()),
						new Function<ResponseStatus, RemotePluginBean>() {
							@Override
							public RemotePluginBean apply(@Nullable ResponseStatus input) {
								return new RemotePluginBean(input.getApplicationLink().getName(), input.getStatus().toString(),
										generateAuthorizationUrl(input.getApplicationLink().createAuthenticatedRequestFactory(), issueId));
							}
						})).build();
	}

	@Nullable
	private String generateAuthorizationUrl(AuthorisationURIGenerator uriGenerator, String issueId)
	{
		final String url = hostApplication.getBaseUrl() + "/secure/SelectTargetProjectAction!default.jspa?id=" + issueId;
		final URI authorisationUri = uriGenerator.getAuthorisationURI(URI.create(url));
		return authorisationUri != null ? authorisationUri.toString() : null;
	}

}
