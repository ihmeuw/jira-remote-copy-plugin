package com.atlassian.cpji.util;

import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestResponseUtil {

    @Mock
    Response response;

    private void setResponseStreamAs(String desiredResponse) throws ResponseException {
        InputStream is = IOUtils.toInputStream(desiredResponse);
        when(response.getResponseBodyAsStream()).thenReturn(is);
    }

    @Test
    public void shouldReturnWholeBodyWhenItDoesntExceedLimit() throws ResponseException {
        final String responseSlightlyGreaterThanBufferSize = StringUtils.repeat("\u33A1", ResponseUtil.DEFAULT_BUFFER_LENGTH + 10);
        setResponseStreamAs(responseSlightlyGreaterThanBufferSize);
        String result = ResponseUtil.getResponseAsTrimmedString(response, ResponseUtil.DEFAULT_BUFFER_LENGTH+11);
        assertEquals(responseSlightlyGreaterThanBufferSize, result);
    }

    /**
     * Produces log error: main ERROR [atlassian.cpji.util.ResponseUtil] Response body exceeded maximum permitted length (10251). Please contact support for assistance.
     * @throws ResponseException
     */
    @Test
    public void shouldReturnTrimmedResponseWhenIsGreatedThanLimit() throws ResponseException {
        //as trimming implementation is not strict, we need to make sure that response is at least one buffer length longer than limit
        final String responseGreaterThanMaximumSize = StringUtils.repeat("\u33A1", ResponseUtil.DEFAULT_BUFFER_LENGTH *3);
        setResponseStreamAs(responseGreaterThanMaximumSize);

        String result = ResponseUtil.getResponseAsTrimmedString(response, ResponseUtil.DEFAULT_BUFFER_LENGTH+11);

        final String trimmedResponse = responseGreaterThanMaximumSize.substring(0, ResponseUtil.DEFAULT_BUFFER_LENGTH*2);
        assertEquals(trimmedResponse, result);
    }

}
