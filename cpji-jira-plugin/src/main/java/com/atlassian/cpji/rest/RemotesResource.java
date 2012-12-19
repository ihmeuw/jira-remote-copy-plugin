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

	public RemotesResource(RemoteJiraService remoteJiraService, JiraProxyFactory jiraProxyFactory) {
		this.remoteJiraService = remoteJiraService;
        this.jiraProxyFactory = jiraProxyFactory;
    }

//	@GET
//	@Path("plugins")
//	public Response getRemotePlugins(@QueryParam("issueId") final String issueId) {
//		return Response.ok(Iterables
//				.transform(Iterables.filter(remoteJiraService.getPluginInfo(), NegativeResponseStatus.onlyRemoteJiras()),
//						new Function<Either<NegativeResponseStatus, SuccessfulResponse>, RemotePluginBean>() {
//							@Override
//							public RemotePluginBean apply(@Nullable Either<NegativeResponseStatus, SuccessfulResponse> input) {
//                                ResultWithJiraLocation<?> result = ResultWithJiraLocation.extract(input);
//                                return RemotePluginBean.create(result, jiraProxyFactory,issueId);
//							}
//						})).build();
//	}

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
		return Response.ok(AvailableProjectsBean.create(jiraProxyFactory, issueId,
				remoteJiraService.getProjects())).build();
	}

}
