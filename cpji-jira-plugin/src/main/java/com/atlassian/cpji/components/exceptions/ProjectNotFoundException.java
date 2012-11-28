package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 *
 * @since v3.0
 */
public class ProjectNotFoundException extends CopyIssueException
{
    public ProjectNotFoundException(final ErrorCollection errorCollection)
    {
        super(errorCollection);
    }
}
