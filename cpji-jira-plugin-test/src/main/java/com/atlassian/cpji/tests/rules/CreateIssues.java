package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.util.concurrent.Promise;
import com.google.common.collect.Lists;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateIssues extends ExternalResource {
	private static final Logger logger = LoggerFactory.getLogger(CreateIssues.class);

	private final JiraRestClient restClient;
	private final List<BasicIssue> issues = Lists.newArrayList();
	private final boolean cleanUp;

	public CreateIssues(JiraRestClient restClient) {
		this(restClient, true);
	}

	public CreateIssues(JiraRestClient restClient, boolean cleanUp) {
		this.restClient = restClient;
		this.cleanUp = cleanUp;
	}

	@Override
	protected void before() throws Throwable {
		super.before();
		issues.clear();
	}

	public Issue newIssue(FieldInput... fieldInput) {
        return newIssue(IssueInput.createWithFields(fieldInput));
	}

    public Issue newIssue(IssueInput input){
        Issue issue = restClient.getIssueClient().getIssue(
				restClient.getIssueClient().createIssue(input).claim().getKey()).claim();
        issues.add(issue);
        return issue;
    }

	@Override
	protected void after() {
		super.after();

		if (cleanUp) {
			for(BasicIssue issue : issues) {
				try {
//					restClient.getIssueClient().deleteIssue(issue.getKey(), true).claim();
				} catch (Exception e) {
					logger.error(String.format("Unable to delete issue %s", issue.getKey()), e);
				}
			}
		}
	}
}
