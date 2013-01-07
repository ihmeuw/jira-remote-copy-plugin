package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;

/**
 * @since v3.0
 */
public class ProjectNotFoundException extends CopyIssueException {
    public ProjectNotFoundException(final ErrorCollection errorCollection) {
        super(errorCollection);
    }

    public ProjectNotFoundException(final String errorMessage){
        this(getErrorCollectionWithMessage(errorMessage));
    }

    private static ErrorCollection getErrorCollectionWithMessage(final String message){
        final SimpleErrorCollection ec = new SimpleErrorCollection();
        ec.addErrorMessage(message);
        return ec;
    }
}
