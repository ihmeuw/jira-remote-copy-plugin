package com.atlassian.cpji.components;

/**
 * Simple result that means nothing more than "OK"
 * @since v3.0
 */
public class SuccessfulResponse extends ResultWithJiraLocation<Void> {
    public SuccessfulResponse(final JiraLocation jiraLocation) {
        super(jiraLocation, null);
    }
}
