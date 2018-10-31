package com.atlassian.cpji.components.model;

import io.atlassian.fugue.Either;

/**
 * Simple result that means nothing more than "OK"
 *
 * @since v3.0
 */
public class SuccessfulResponse extends ResultWithJiraLocation<String> {
    private SuccessfulResponse(final JiraLocation jiraLocation) {
        super(jiraLocation, "ok");
    }

    public static SuccessfulResponse build(final JiraLocation jiraLocation) {
        return new SuccessfulResponse(jiraLocation);
    }

    public static Either<NegativeResponseStatus, SuccessfulResponse> buildEither(final JiraLocation jiraLocation) {
        return Either.right(new SuccessfulResponse(jiraLocation));
    }
}
