package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import javax.annotation.Nonnull;
import java.util.Map;

public class RemoteProjectService {

	private static final Logger log = Logger.getLogger(RemoteProjectService.class);

	private final ApplicationLinkService applicationLinkService;

	public RemoteProjectService(final ApplicationLinkService applicationLinkService) {
		this.applicationLinkService = applicationLinkService;
	}

	// @todo should be multi-threaded
	@Nonnull
	public Map<ApplicationLink, Either<ResponseStatus, Iterable<BasicProject>>> getProjects() {
		Map<ApplicationLink, Either<ResponseStatus, Iterable<BasicProject>>> result = Maps.newHashMap();
		for (ApplicationLink jira : applicationLinkService.getApplicationLinks(JiraApplicationType.class)) {
			result.put(jira, getProjects(jira));
		}
		return result;
	}

	@Nonnull
	public Either<ResponseStatus, Iterable<BasicProject>> getProjects(ApplicationLink jiraServer) {
		try {
			ApplicationLinkRequest request = jiraServer.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "/rest/api/latest/project");
			return request.execute(new ApplicationLinkResponseHandler<Either<ResponseStatus, Iterable<BasicProject>>>()
			{
				public Either<ResponseStatus, Iterable<BasicProject>> credentialsRequired(final Response response) throws ResponseException
				{
					return Either.left(ResponseStatus.AUTHORIZATION_REQUIRED);
				}

				public Either<ResponseStatus, Iterable<BasicProject>> handle(final Response response) throws ResponseException
				{
					if (log.isDebugEnabled())
					{
						log.debug("Response is: " + response.getResponseBodyAsString());
					}
					if (response.getStatusCode() == 401)
					{
						return Either.left(ResponseStatus.AUTHENTICATION_FAILED);
					}
					try {
						return Either.right(new BasicProjectsJsonParser().parse(new JSONArray(new JSONTokener(response.getResponseBodyAsString()))));
					} catch (JSONException e) {
						log.error("Failed to parse JSON", e);
						return Either.left(ResponseStatus.COMMUNICATION_FAILED);
					}
				}
			});
		}
		catch (CredentialsRequiredException ex)
		{
			return Either.left(ResponseStatus.AUTHORIZATION_REQUIRED);
		} catch (ResponseException e) {
			log.error("Failed to transform response", e);
			return Either.left(ResponseStatus.COMMUNICATION_FAILED);
		}
	}

}
