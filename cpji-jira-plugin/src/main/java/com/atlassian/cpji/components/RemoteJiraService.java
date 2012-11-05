package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.rest.client.domain.ServerInfo;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.jira.rest.client.internal.json.ServerInfoJsonParser;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import javax.annotation.Nonnull;
import java.util.Map;

public class RemoteJiraService {

	private static final Logger log = Logger.getLogger(RemoteJiraService.class);

	private final ApplicationLinkService applicationLinkService;

	public RemoteJiraService(final ApplicationLinkService applicationLinkService) {
		this.applicationLinkService = applicationLinkService;
	}

	@Nonnull
	public Map<ApplicationLink, Either<ResponseStatus, ServerInfo>> getServers() {
		final Map<ApplicationLink, Either<ResponseStatus, ServerInfo>> result = Maps.newHashMap();
		for (ApplicationLink jira : applicationLinkService.getApplicationLinks(JiraApplicationType.class)) {
			result.put(jira, getServerInfo(jira));
		}
		return result;
	}

	private Either<ResponseStatus, ServerInfo> getServerInfo(ApplicationLink jiraServer) {
		return callRestService(jiraServer, "/rest/api/latest/serverInfo", new AbstractJsonResponseHandler<ServerInfo>() {
			@Override
			protected ServerInfo parseResponse(Response response) throws ResponseException, JSONException {
				return new ServerInfoJsonParser().parse(new JSONObject(new JSONTokener(response.getResponseBodyAsString())));
			}
		});
	}

	// @todo should be multi-threaded
	@Nonnull
	public Map<ApplicationLink, Either<ResponseStatus, Iterable<BasicProject>>> getProjects() {
		final Map<ApplicationLink, Either<ResponseStatus, Iterable<BasicProject>>> result = Maps.newHashMap();
		for (ApplicationLink jira : applicationLinkService.getApplicationLinks(JiraApplicationType.class)) {
			result.put(jira, getProjects(jira));
		}
		return result;
	}

	@Nonnull
	public Either<ResponseStatus, Iterable<BasicProject>> getProjects(ApplicationLink jiraServer) {
		return callRestService(jiraServer, "/rest/api/latest/project", new AbstractJsonResponseHandler<Iterable<BasicProject>>() {
			@Override
			protected Iterable<BasicProject> parseResponse(Response response) throws ResponseException, JSONException {
				return new BasicProjectsJsonParser().parse(
						new JSONArray(new JSONTokener(response.getResponseBodyAsString())));
			}
		});
	}

	private <T> Either<ResponseStatus, T> callRestService(final ApplicationLink jiraServer, final String path, final AbstractJsonResponseHandler handler) {
		try {
			ApplicationLinkRequest request = jiraServer.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, path);
			return (Either<ResponseStatus, T>) request.execute(handler);
		}
		catch (CredentialsRequiredException ex)
		{
			return Either.left(ResponseStatus.AUTHORIZATION_REQUIRED);
		} catch (ResponseException e) {
			log.error("Failed to transform response", e);
			return Either.left(ResponseStatus.COMMUNICATION_FAILED);
		}
	}

	protected static abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<ResponseStatus, T>>
	{
		public Either<ResponseStatus, T> credentialsRequired(final Response response) throws ResponseException
		{
			return Either.left(ResponseStatus.AUTHORIZATION_REQUIRED);
		}

		public Either<ResponseStatus, T> handle(final Response response) throws ResponseException
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
				return Either.right(parseResponse(response));
			} catch (JSONException e) {
				log.error("Failed to parse JSON", e);
				return Either.left(ResponseStatus.COMMUNICATION_FAILED);
			}
		}

		protected abstract T parseResponse(Response response) throws ResponseException, JSONException;
	}

}
