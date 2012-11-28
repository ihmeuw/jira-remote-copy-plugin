package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 *
 * @since v3.0
 */
public class CreationException extends CopyIssueException
{
    public CreationException(final ErrorCollection errorCollection)
    {
        super(errorCollection);
    }
}
