package com.atlassian.cpji.rest.model;

import java.util.List;

/**
*
* @since v3.0
*/
public class CreationResult
{
    public boolean success;
    public List<String> errorMessages;

    public CreationResult(final boolean success)
    {
        this.success = success;
    }

    public CreationResult(final boolean success, final List<String> errorMessages)
    {
        this.success = success;
        this.errorMessages = errorMessages;
    }
}
