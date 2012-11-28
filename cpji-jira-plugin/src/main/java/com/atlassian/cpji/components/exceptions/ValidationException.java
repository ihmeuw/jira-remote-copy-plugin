package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 *
 * @since v3.0
 */
public class ValidationException extends CopyIssueException
{
    public ValidationException(final ErrorCollection errorCollection)
    {
        super(errorCollection);
    }
}
