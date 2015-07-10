package com.atlassian.cpji.util;

import com.atlassian.jira.util.ErrorCollection;

import javax.annotation.Nullable;

/**
 * Represents a response from a JIRA REST resource.
 */
public class RestResponse
{
    private final ErrorCollection errors;
    private final int statusCode;
    private final String statusText;
    private final boolean successful;

    public RestResponse(@Nullable final ErrorCollection errors, final int statusCode, final String statusText, final boolean successful)
    {
        this.errors = errors;
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.successful = successful;
    }

    /**
     * Returns true if the response entity was a non-empty ErrorCollection, false if otherwise.
     *
     * @return true if the response entity was a non-empty ErrorCollection, false if otherwise
     */
    public boolean hasErrors()
    {
        return (errors != null && errors.hasAnyErrors());
    }

    /**
     * Returns the ErrorCollection from the response entity. If the response was successful, this will generally be null.
     *
     * @return the ErrorCollection from the response entity
     */
    public ErrorCollection getErrors()
    {
        return errors;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusText()
    {
        return statusText;
    }

    public boolean isSuccessful()
    {
        return successful && !hasErrors();
    }
}
