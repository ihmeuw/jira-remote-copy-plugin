package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 * @since v3.0
 */
public class RemoteLinksNotFoundException extends CopyIssueException {

    public RemoteLinksNotFoundException(final ErrorCollection errorCollection) {
        super(errorCollection);
    }
}
