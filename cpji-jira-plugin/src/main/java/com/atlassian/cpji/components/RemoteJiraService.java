package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.cpji.action.CopyIssueToInstanceAction;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RemoteJiraService {

	private static final Logger log = Logger.getLogger(RemoteJiraService.class);
	private static final int THREADS = 5;

	private final ApplicationLinkService applicationLinkService;

	public RemoteJiraService(final ApplicationLinkService applicationLinkService) {
		this.applicationLinkService = applicationLinkService;
	}

	/**
	 * Asks each JIRA to see what RIC plugin version do they have installed.
	 * @return
	 */
	@Nonnull
	public Iterable<ResponseStatus> getPluginInfo() {
		final ExecutorService es = Executors.newFixedThreadPool(THREADS);
		final Iterable<ApplicationLink> applicationLinks = getJiraAppLinks();

		final List<Callable<ResponseStatus>> queries = Lists.newArrayList(
				Iterables.transform(applicationLinks,  new Function<ApplicationLink, Callable<ResponseStatus>>() {
					@Override
					public Callable<ResponseStatus> apply(final ApplicationLink applicationLink) {
						final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();

						return new Callable<ResponseStatus>() {
							@Override
							public ResponseStatus call() throws Exception {
								return getPluginInfo(applicationLink,  requestFactory);
							}
						};
					}
				})
		);

		try {
			return ImmutableList.copyOf(Iterables.transform(es.invokeAll(queries), new Function<Future<ResponseStatus>, ResponseStatus>() {
				@Override
				public ResponseStatus apply(@Nullable Future<ResponseStatus> input) {
					try {
						return input.get();
					} catch (Exception e) {
						log.warn("Failed to execute Application Links request", e);
						return ResponseStatus.communicationFailed(null);
					}
				}
			}));
		} catch (InterruptedException e) {
			log.warn("Threads were interrupted during Application Links request", e);
			return Collections.emptyList();
		} finally {
			es.shutdown();
		}
	}

	@Nonnull
	public ResponseStatus getPluginInfo(final ApplicationLink applicationLink, ApplicationLinkRequestFactory requestFactory) {
		ApplicationLinkRequest request;
		try {
			request = requestFactory.createRequest(
					Request.MethodType.GET, CopyIssueToInstanceAction.REST_URL_COPY_ISSUE + PluginInfoResource.RESOURCE_PATH);
		} catch (CredentialsRequiredException e) {
			return ResponseStatus.authorizationRequired(applicationLink);
		}
		try {
			return request.execute(new ApplicationLinkResponseHandler<ResponseStatus>()
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
						return ResponseStatus.authenticationFailed(applicationLink);
					}
					if (StringUtils.equalsIgnoreCase(PluginInfoResource.PLUGIN_INSTALLED, response.getResponseBodyAsString()))
					{
						log.debug(String.format("Remote JIRA instance '%s' has the CPJI plugin installed.", RemoteJiraService.toString(
								applicationLink)));
						return ResponseStatus.ok(applicationLink);
					}
					log.debug(String.format("Remote JIRA instance '%s' has the CPJI plugin NOT installed.", RemoteJiraService.toString(applicationLink)));
					return ResponseStatus.pluginNotInstalled(applicationLink);
				}
			});
		} catch (ResponseException e) {
			return ResponseStatus.communicationFailed(applicationLink);
		}
	}

	protected Iterable<ApplicationLink> getJiraAppLinks() {
		return applicationLinkService.getApplicationLinks(
				JiraApplicationType.class);
	}

	@Nonnull
	public Iterable<Either<ResponseStatus, Projects>> getProjects() {
		final ExecutorService es = Executors.newFixedThreadPool(THREADS);
		final Iterable<ApplicationLink> applicationLinks = getJiraAppLinks();

		final List<Callable<Either<ResponseStatus, Projects>>> queries = Lists.newArrayList(
				Iterables.transform(applicationLinks,
						new Function<ApplicationLink, Callable<Either<ResponseStatus, Projects>>>() {
							@Override
							public Callable<Either<ResponseStatus,Projects>> apply(final ApplicationLink applicationLink) {
								final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();

								return new Callable<Either<ResponseStatus, Projects>>() {
									@Override
									public Either<ResponseStatus, Projects> call() {
										return getProjects(applicationLink, requestFactory);
									}
								};
							}
						})
		);

		try {
			return ImmutableList.copyOf(Iterables.transform(es.invokeAll(queries),
					new Function<Future<Either<ResponseStatus, Projects>>, Either<ResponseStatus, Projects>>() {
						@Override
						public Either<ResponseStatus, Projects> apply(Future<Either<ResponseStatus, Projects>> eitherFuture) {
							try {
								return eitherFuture.get();
							} catch (Exception e) {
								log.warn("Failed to execute Application Links request", e);
								return Either.left(ResponseStatus.communicationFailed(null));
							}
						}
					}));
		} catch (InterruptedException e) {
			log.warn("Threads were interrupted during Application Links request", e);
			return Collections.emptyList();
		} finally {
			es.shutdown();
		}
	}

	@Nonnull
	public Either<ResponseStatus, Projects> getProjects(ApplicationLink jiraServer) {
		return getProjects(jiraServer, jiraServer.createAuthenticatedRequestFactory());
	}

	@Nonnull
	protected Either<ResponseStatus, Projects> getProjects(final ApplicationLink applicationLink, ApplicationLinkRequestFactory requestFactory) {
		return callRestService(applicationLink, requestFactory, "/rest/copyissue/1.0/project", new AbstractJsonResponseHandler<Projects>(
				applicationLink) {
			@Override
			protected Projects parseResponse(Response response) throws ResponseException, JSONException {
				return new Projects(applicationLink, new BasicProjectsJsonParser().parse(
						new JSONArray(new JSONTokener(response.getResponseBodyAsString()))));
			}
		});
	}

	private <T> Either<ResponseStatus, T> callRestService(final ApplicationLink applicationLink,
			final ApplicationLinkRequestFactory requestFactory, final String path, final AbstractJsonResponseHandler handler) {
		try {
			ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, path);
			return (Either<ResponseStatus, T>) request.execute(handler);
		}
		catch (CredentialsRequiredException ex)
		{
			return Either.left(ResponseStatus.authorizationRequired(applicationLink));
		} catch (ResponseException e) {
			return Either.left(createCommunicationFailedStatus(applicationLink, e));
		}
	}

	private ResponseStatus createCommunicationFailedStatus(ApplicationLink applicationLink, ResponseException e) {
		log.error(String.format("Failed to transform response from Application Link: %s (%s)", toString(applicationLink), e.getMessage()));
		return ResponseStatus.communicationFailed(applicationLink);
	}

	protected static abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<ResponseStatus, T>>
	{
		private final ApplicationLink applicationLink;

		protected AbstractJsonResponseHandler(ApplicationLink applicationLink) {
			this.applicationLink = applicationLink;
		}

		public Either<ResponseStatus, T> credentialsRequired(final Response response) throws ResponseException
		{
			return Either.left(ResponseStatus.authorizationRequired(applicationLink));
		}

		public Either<ResponseStatus, T> handle(final Response response) throws ResponseException
		{
			if (log.isDebugEnabled())
			{
				log.debug("Response is: " + response.getResponseBodyAsString());
			}
			if (response.getStatusCode() == 401)
			{
				return Either.left(ResponseStatus.authenticationFailed(applicationLink));
			}
			try {
				return Either.right(parseResponse(response));
			} catch (JSONException e) {
				log.error(String.format("Failed to parse JSON from Application Link: %s (%s)", RemoteJiraService.toString(applicationLink), e.getMessage()));
				return Either.left(ResponseStatus.communicationFailed(applicationLink));
			}
		}

		protected abstract T parseResponse(Response response) throws ResponseException, JSONException;
	}

	protected static String toString(ApplicationLink applicationLink) {
		return applicationLink.getName() + " " + applicationLink.getId() + " " + applicationLink.getDisplayUrl();
	}
}
