package com.atlassian.cpji.components.model;

import com.atlassian.fugue.Either;

/**
 * @since v2.1
 */
public class ResultWithJiraLocation<T> {

    private final JiraLocation jiraLocation;
    private final T result;


    public ResultWithJiraLocation(final JiraLocation jiraLocation, final T result) {
        this.jiraLocation = jiraLocation;
        this.result = result;
    }

    public JiraLocation getJiraLocation() {
        return jiraLocation;
    }

    public T getResult() {
        return result;
    }

    public static ResultWithJiraLocation<?> extract(Either<? extends ResultWithJiraLocation<?>, ? extends ResultWithJiraLocation<?>> either) {
        if (either.isLeft()) {
            return (ResultWithJiraLocation<?>) either.left().get();
        } else {
            return (ResultWithJiraLocation<?>) either.right().get();
        }
    }
}
