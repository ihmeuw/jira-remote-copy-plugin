package com.atlassian.cpji.components.model;

import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
public class ResponseStatus extends ResultWithJiraLocation<ResponseStatus.Status> {

    private ErrorCollection errors = null;

    public ResponseStatus(final JiraLocation jiraLocation, final Status result) {
        super(jiraLocation, result);
    }


    public static ResponseStatus communicationFailed(JiraLocation jiraLocation) {
        return new ResponseStatus(jiraLocation, Status.COMMUNICATION_FAILED);
    }

    public static ResponseStatus authenticationFailed(JiraLocation jiraLocation) {
        return new ResponseStatus(jiraLocation, Status.AUTHENTICATION_FAILED);
    }

    /**
     * @deprecated should use {@link SuccessfulResponse} instead
     */
    @Deprecated
    public static ResponseStatus ok(JiraLocation jiraLocation) {
        return new ResponseStatus(jiraLocation, Status.OK);
    }

    public static ResponseStatus pluginNotInstalled(JiraLocation jiraLocation) {
        return new ResponseStatus(jiraLocation, Status.PLUGIN_NOT_INSTALLED);
    }

    public static ResponseStatus errorOccured(JiraLocation jiraLocation, ErrorCollection errors) {
        ResponseStatus result = new ResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = errors;
        return result;
    }

    public static ResponseStatus errorOccured(JiraLocation jiraLocation, ErrorBean errors) {
        ResponseStatus result = new ResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = new SimpleErrorCollection();
        result.errors.addErrorMessages(errors.getErrors());
        return result;
    }


    public static ResponseStatus errorOccured(JiraLocation jiraLocation, String singleError) {
        ResponseStatus result = new ResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = new SimpleErrorCollection();
        result.errors.addErrorMessage(singleError);
        return result;
    }

    public enum Status {
        COMMUNICATION_FAILED,
        AUTHORIZATION_REQUIRED,
        AUTHENTICATION_FAILED,
        PLUGIN_NOT_INSTALLED,
        ERROR_OCCURRED,
        OK
    }

    public static ResponseStatus authorizationRequired(JiraLocation jiraLocation) {
        return new ResponseStatus(jiraLocation, Status.AUTHORIZATION_REQUIRED);
    }

    public ErrorCollection getErrorCollection() {
        return errors;
    }

    @Nonnull
    public static Predicate<Either<ResponseStatus, SuccessfulResponse>> onlyRemoteJiras() {
        final Predicate<JiraLocation> isLocal = JiraProxyFactory.isLocalLocation();
        return new Predicate<Either<ResponseStatus, SuccessfulResponse>>() {
            @Override
            public boolean apply(@Nullable Either<ResponseStatus, SuccessfulResponse> input) {
                ResultWithJiraLocation<?> result = ResultWithJiraLocation.extract(input);
                final JiraLocation jiraLocation = result.getJiraLocation();
                return jiraLocation != null && !isLocal.apply(jiraLocation);
            }
        };
    }
}
