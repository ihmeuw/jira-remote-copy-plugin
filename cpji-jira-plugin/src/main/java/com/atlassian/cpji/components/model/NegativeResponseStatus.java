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
 * @since v5.2
 */
public class NegativeResponseStatus extends ResultWithJiraLocation<NegativeResponseStatus.Status> {

    private ErrorCollection errors = null;

    public NegativeResponseStatus(final JiraLocation jiraLocation, final Status result) {
        super(jiraLocation, result);
    }


    public static NegativeResponseStatus communicationFailed(JiraLocation jiraLocation) {
        return new NegativeResponseStatus(jiraLocation, Status.COMMUNICATION_FAILED);
    }

    public static NegativeResponseStatus authenticationFailed(JiraLocation jiraLocation) {
        return new NegativeResponseStatus(jiraLocation, Status.AUTHENTICATION_FAILED);
    }

    public static NegativeResponseStatus pluginNotInstalled(JiraLocation jiraLocation) {
        return new NegativeResponseStatus(jiraLocation, Status.PLUGIN_NOT_INSTALLED);
    }

    public static NegativeResponseStatus errorOccured(JiraLocation jiraLocation, ErrorCollection errors) {
        NegativeResponseStatus result = new NegativeResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = errors;
        return result;
    }

    public static NegativeResponseStatus errorOccured(JiraLocation jiraLocation, ErrorBean errors) {
        NegativeResponseStatus result = new NegativeResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = new SimpleErrorCollection();
        result.errors.addErrorMessages(errors.getErrors());
        return result;
    }


    public static NegativeResponseStatus errorOccured(JiraLocation jiraLocation, String singleError) {
        NegativeResponseStatus result = new NegativeResponseStatus(jiraLocation, Status.ERROR_OCCURRED);
        result.errors = new SimpleErrorCollection();
        result.errors.addErrorMessage(singleError);
        return result;
    }

    public enum Status {
        COMMUNICATION_FAILED,
        AUTHORIZATION_REQUIRED,
        AUTHENTICATION_FAILED,
        PLUGIN_NOT_INSTALLED,
        ERROR_OCCURRED
    }

    public static NegativeResponseStatus authorizationRequired(JiraLocation jiraLocation) {
        return new NegativeResponseStatus(jiraLocation, Status.AUTHORIZATION_REQUIRED);
    }

    public ErrorCollection getErrorCollection() {
        return errors;
    }

    @Nonnull
    public static Predicate<Either<NegativeResponseStatus, SuccessfulResponse>> onlyRemoteJiras() {
        final Predicate<JiraLocation> isLocal = JiraProxyFactory.isLocalLocation();
        return new Predicate<Either<NegativeResponseStatus, SuccessfulResponse>>() {
            @Override
            public boolean apply(@Nullable Either<NegativeResponseStatus, SuccessfulResponse> input) {
                ResultWithJiraLocation<?> result = ResultWithJiraLocation.extract(input);
                final JiraLocation jiraLocation = result.getJiraLocation();
                return jiraLocation != null && !isLocal.apply(jiraLocation);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NegativeResponseStatus(").append(getResult());
        if(errors != null){
            sb.append(", errors: ").append(errors);
        }
        sb.append(")");
        return sb.toString();
    }
}
