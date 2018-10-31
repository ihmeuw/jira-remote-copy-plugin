package com.atlassian.cpji.components.model;

import io.atlassian.fugue.Either;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * @since v2.1
 */
public class ResultWithJiraLocation<T> {

    private final JiraLocation jiraLocation;
    private final T result;

    public ResultWithJiraLocation(final JiraLocation jiraLocation, final T result) {
		Preconditions.checkNotNull(jiraLocation);
		Preconditions.checkNotNull(result);
        this.jiraLocation = jiraLocation;
        this.result = result;
    }

	@Nonnull
    public JiraLocation getJiraLocation() {
        return jiraLocation;
    }

	@Nonnull
    public T getResult() {
        return result;
    }

    public static ResultWithJiraLocation<?> extract(Either<? extends ResultWithJiraLocation<?>, ? extends ResultWithJiraLocation<?>> either) {
        if (either.isLeft()) {
            return either.left().get();
        } else {
            return either.right().get();
        }
    }
}
