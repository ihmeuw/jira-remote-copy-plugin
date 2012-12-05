package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.ResponseStatus;
import com.atlassian.cpji.components.model.ResultWithJiraLocation;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.fugue.Either;
import com.atlassian.jira.util.ErrorCollection;

import static junit.framework.Assert.*;

/**
 * @since v3.0
 */
public class EitherTestingUtils {
    public static ResponseStatus verifyFailure(Either<ResponseStatus, ?> either, JiraLocation location){
        assertTrue(either.isLeft());
        final ResponseStatus responseStatus = (ResponseStatus) either.left().get();
        assertNotNull(responseStatus);
        assertSame(location, responseStatus.getJiraLocation());
        assertEquals(responseStatus.getResult(), ResponseStatus.Status.ERROR_OCCURRED);
        return responseStatus;
    }

    public static ResponseStatus verifyFailure(Either<ResponseStatus, ?> either, ErrorCollection errors, JiraLocation location){
        ResponseStatus status = verifyFailure(either, location);
        assertSame(errors, status.getErrorCollection());
        return status;
    }


    public static SuccessfulResponse verifySuccess(Either<ResponseStatus, SuccessfulResponse> either, JiraLocation location){
        final SuccessfulResponse response = extractResult(either, location);
        assertTrue(response != null);
        return response;

    }

    public static <T extends ResultWithJiraLocation<?>> T extractResult(Either<ResponseStatus, T > either, JiraLocation location){
        final T result = extractRight(either);
        assertSame(location, result.getJiraLocation());
        return result;
    }


    public static <T> T extractRight(Either<ResponseStatus, T > either){
        assertTrue(either.isRight());
        final T result = (T) either.right().get();
        assertNotNull(result);
        return result;
    }
}
