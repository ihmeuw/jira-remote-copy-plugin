package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.ResultWithJiraLocation;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import io.atlassian.fugue.Either;
import com.atlassian.jira.util.ErrorCollection;

import static junit.framework.Assert.*;

/**
 * @since v3.0
 */
public class EitherTestingUtils {
    public static NegativeResponseStatus verifyFailure(Either<NegativeResponseStatus, ?> either, JiraLocation location){
        assertTrue(either.isLeft());
        final NegativeResponseStatus responseStatus = (NegativeResponseStatus) either.left().get();
        assertNotNull(responseStatus);
        assertSame(location, responseStatus.getJiraLocation());
        assertEquals(responseStatus.getResult(), NegativeResponseStatus.Status.ERROR_OCCURRED);
        return responseStatus;
    }

    public static NegativeResponseStatus verifyFailure(Either<NegativeResponseStatus, ?> either, ErrorCollection errors, JiraLocation location){
        NegativeResponseStatus status = verifyFailure(either, location);
        assertSame(errors, status.getErrorCollection());
        return status;
    }


    public static SuccessfulResponse verifySuccess(Either<NegativeResponseStatus, SuccessfulResponse> either, JiraLocation location){
        final SuccessfulResponse response = extractResult(either, location);
        assertTrue(response != null);
        return response;

    }

    public static <T extends ResultWithJiraLocation<?>> T extractResult(Either<NegativeResponseStatus, T > either, JiraLocation location){
        final T result = extractRight(either);
        assertSame(location, result.getJiraLocation());
        return result;
    }


    public static <T> T extractRight(Either<NegativeResponseStatus, T > either){
        assertTrue(either.isRight());
        final T result = (T) either.right().get();
        assertNotNull(result);
        return result;
    }
}
