package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.fugue.Either;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

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
	private final JiraAuthenticationContext authenticationContext;
    private final JiraBaseUrls baseUrls;
    private final PermissionManager permissionManager;
    private final JiraProxyFactory jiraProxyFactory;

	public RemoteJiraService(final ApplicationLinkService applicationLinkService, final JiraAuthenticationContext authenticationContext, final JiraBaseUrls baseUrls, final PermissionManager permissionManager, JiraProxyFactory jiraProxyFactory) {
		this.applicationLinkService = applicationLinkService;
		this.authenticationContext = authenticationContext;
        this.baseUrls = baseUrls;
        this.permissionManager = permissionManager;
        this.jiraProxyFactory = jiraProxyFactory;
    }

	/**
	 * Asks each JIRA to see what RIC plugin version do they have installed.
	 * @return
	 */
	@Nonnull
	public Iterable<ResponseStatus> getPluginInfo() {

        return executeForEveryJira(new FunctionWithFallback<ResponseStatus>(){

            @Override
            public ResponseStatus onInvocationException(Exception e) {
                log.warn("Failed to execute Application Links request", e);
                return ResponseStatus.communicationFailed(null);
            }

            @Override
            public ResponseStatus apply(@Nullable JiraProxy input) {
                return input.isPluginInstalled();
            }
        });

	}


    private interface FunctionWithFallback<T> extends Function<JiraProxy, T>{
        T onInvocationException(Exception e);
    }

    private <T>Iterable<T> executeForEveryJira(final FunctionWithFallback<T> function){
        final ExecutorService es = Executors.newFixedThreadPool(THREADS);
        final Iterable<JiraProxy> applicationLinks = jiraProxyFactory.getAllJiraProxies();
        final User user = authenticationContext.getLoggedInUser();
        final List<Callable<T>> queries = Lists.newArrayList(
                Iterables.transform(applicationLinks,
                        new Function<JiraProxy, Callable<T>>()
                        {
                            @Override
                            public Callable<T> apply(final JiraProxy applicationLink)
                            {
                                return new Callable<T>()
                                {
                                    @Override
                                    public T call()
                                    {
                                        ComponentManager.getInstance().getJiraAuthenticationContext().setLoggedInUser(user);
                                        return function.apply(applicationLink);
                                    }
                                };
                            }
                        })
        );
        try {
            return ImmutableList.copyOf(Iterables.transform(es.invokeAll(queries),
                    new Function<Future<T>, T>() {
                        @Override
                        public T apply(Future<T> eitherFuture) {
                            try {
                                return eitherFuture.get();
                            } catch (Exception e) {
                                return function.onInvocationException(e);
                            }
                        }
                    }));
        } catch (Exception e) {
            log.warn("Threads were interrupted during Application Links request", e);
            return Collections.emptyList();
        } finally {
            es.shutdown();
        }
    }


	@Nonnull
	public Iterable<Either<ResponseStatus, Projects>> getProjects() {

        return executeForEveryJira(new FunctionWithFallback<Either<ResponseStatus, Projects>>() {
            @Override
            public Either<ResponseStatus, Projects> onInvocationException(Exception e) {
                return Either.left(ResponseStatus.communicationFailed(null));
            }

            @Override
            public Either<ResponseStatus, Projects> apply(JiraProxy input) {
                return input.getProjects();
            }
        });
 	}

}
