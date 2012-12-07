package com.atlassian.cpji.components.exceptions;

import com.atlassian.jira.util.ErrorCollection;

/**
 * @since v3.0
 */
abstract public class CopyIssueException extends Exception {

    private final ErrorCollection errorCollection;

    public CopyIssueException(final ErrorCollection errorCollection) {
        this.errorCollection = errorCollection;
    }


    public ErrorCollection getErrorCollection() {
        return errorCollection;
    }


    @Override
    public String getMessage() {
        if(errorCollection != null){
            return errorCollection.toString();
        } else {
            return super.getMessage();
        }
    }
}
