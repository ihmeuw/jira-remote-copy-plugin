package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.ProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import com.atlassian.jira.rest.client.domain.input.IssueInput;
import com.google.common.collect.Lists;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateIssues extends ExternalResource {
	private static final Logger logger = LoggerFactory.getLogger(CreateIssues.class);

	private static final ProgressMonitor NPM = new NullProgressMonitor();
	private final JiraRestClient restClient;
	private final List<BasicIssue> issues = Lists.newArrayList();

	public CreateIssues(JiraRestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	protected void before() throws Throwable {
		super.before();
		issues.clear();
	}

	public Issue newIssue(FieldInput... fieldInput) {
		Issue issue = restClient.getIssueClient().getIssue(
				restClient.getIssueClient().createIssue(IssueInput.createWithFields(fieldInput), NPM).getKey(), NPM);
		issues.add(issue);
		return issue;
	}

	@Override
	protected void after() {
		super.after();

		for(BasicIssue issue : issues) {
			try {
				restClient.getIssueClient().removeIssue(issue, true, NPM);
			} catch (Exception e) {
				logger.error(String.format("Unable to delete issue %s", issue.getKey()), e);
			}
		}
	}
}
