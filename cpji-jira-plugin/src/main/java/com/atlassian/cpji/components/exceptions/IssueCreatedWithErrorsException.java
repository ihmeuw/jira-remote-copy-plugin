package com.atlassian.cpji.components.exceptions;

import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.jira.util.ErrorCollection;

/**
 * @since v3.0
 */
public class IssueCreatedWithErrorsException extends CopyIssueException {
    private final IssueCreationResultBean result;

    public IssueCreatedWithErrorsException(final IssueCreationResultBean result, final ErrorCollection errors) {
        super(errors);
        this.result = result;
    }

    public IssueCreationResultBean getResult() {
        return result;
    }

}
