package com.atlassian.cpji.components;

import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.PluginVersion;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.util.WorkContextUtil;
import com.atlassian.fugue.Either;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteJiraService {

    private static final Logger log = Logger.getLogger(RemoteJiraService.class);
    private static final int THREADS = 5;

    private final JiraAuthenticationContext authenticationContext;
    private final JiraProxyFactory jiraProxyFactory;

    public RemoteJiraService(final JiraAuthenticationContext authenticationContext, JiraProxyFactory jiraProxyFactory) {
        this.authenticationContext = authenticationContext;
        this.jiraProxyFactory = jiraProxyFactory;
    }

    /**
     * Asks each JIRA to see what RIC plugin version do they have installed.
     *
     * @return
     */
    @Nonnull
    public Iterable<Either<NegativeResponseStatus, PluginVersion>> getPluginInfo() {

        return executeForEveryJira(new FunctionWithFallback<Either<NegativeResponseStatus, PluginVersion>>() {

            @Override
            public Either<NegativeResponseStatus, PluginVersion> onInvocationException(Exception e) {
                log.warn("Failed to execute Application Links request", e);
                return Either.left(NegativeResponseStatus.communicationFailed(JiraLocation.LOCAL));
            }

            @Override
            public Either<NegativeResponseStatus, PluginVersion> apply(JiraProxy input) {
                return input.isPluginInstalled();
            }
        });

    }


    private interface FunctionWithFallback<T> extends Function<JiraProxy, T> {
        T onInvocationException(Exception e);
    }

    private <T> Iterable<T> executeForEveryJira(final FunctionWithFallback<T> function) {
        final ExecutorService es = Executors.newFixedThreadPool(THREADS);
        final Iterable<JiraProxy> applicationLinks = jiraProxyFactory.getAllJiraProxies();
        final ApplicationUser user = authenticationContext.getLoggedInUser();
        final List<Callable<T>> queries = Lists.newArrayList(
                Iterables.transform(applicationLinks,
                        applicationLink -> () -> WorkContextUtil.runWithNewWorkContextIfAvailable(() -> {
                            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
                            return function.apply(applicationLink);
                        }))
        );
        try {
            return ImmutableList.copyOf(Iterables.transform(es.invokeAll(queries),
                    eitherFuture -> {
                        try {
                            return eitherFuture.get();
                        } catch (Exception e) {
                            return function.onInvocationException(e);
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
    public Iterable<Either<NegativeResponseStatus, Projects>> getProjects() {

        return executeForEveryJira(new FunctionWithFallback<Either<NegativeResponseStatus, Projects>>() {
            @Override
            public Either<NegativeResponseStatus, Projects> onInvocationException(Exception e) {
                return Either.left(NegativeResponseStatus.communicationFailed(JiraLocation.LOCAL));
            }

            @Override
            public Either<NegativeResponseStatus, Projects> apply(JiraProxy input) {
                Either<NegativeResponseStatus, PluginVersion> version = input.isPluginInstalled();
                if(version.isLeft())
                    return Either.left(version.left().get());

                if(version.right().get().getResult().equals(PluginInfoResource.PLUGIN_VERSION)){
                    return input.getProjects();
                } else {
                    return Either.left(NegativeResponseStatus.unsupportedVersion(input.getJiraLocation()));
                }
            }
        });
    }

}
