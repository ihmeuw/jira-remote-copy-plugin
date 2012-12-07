package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 * @since v3.0
 */
public class IssueLinkCreationException extends CopyIssueException {
    public IssueLinkCreationException(final ErrorCollection errorCollection) {
        super(errorCollection);
    }
}
