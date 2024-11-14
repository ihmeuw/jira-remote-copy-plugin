package com.atlassian.cpji.rest;

import com.atlassian.sal.api.net.ResponseException;
import javax.inject.Inject;

/**
 * Exception that's thrown when the response contains the status code 401 Unauthorized.
 * @since v1.3
 */
public class UnauthorizedResponseException extends ResponseException
{
    @Inject
    public UnauthorizedResponseException()
    {
    }
}
