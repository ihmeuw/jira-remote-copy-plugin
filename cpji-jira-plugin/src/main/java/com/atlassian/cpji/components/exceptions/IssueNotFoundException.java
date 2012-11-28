package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 *
 * @since v3.0
 */
public class IssueNotFoundException extends CopyIssueException
{
    public IssueNotFoundException(final ErrorCollection errorCollection)
    {
        super(errorCollection);
    }
}
