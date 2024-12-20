package com.atlassian.cpji.components.model;

import com.atlassian.jira.rest.client.api.domain.BasicProject;

/**
 * @since v2.1
 */
public class Projects extends ResultWithJiraLocation<Iterable<BasicProject>> {
    public Projects(JiraLocation applicationLink, Iterable<BasicProject> result) {
        super(applicationLink, result);
    }
}
