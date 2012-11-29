package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

/**
 * @since v5.2
 */
public class PermissionViolationPage extends AbstractJiraPage {
	private final String path;

	public PermissionViolationPage(String path) {
		this.path = StringUtils.startsWith(path, "/") ? path : ('/' + path);
	}

	@Override
	public TimedCondition isAt() {
		return Conditions.forMatcher(elementFinder.find(By.cssSelector(".form-body header h1")).timed().getText(),
				Matchers.equalToIgnoringCase("Permission Violation"));
	}

	@Override
	public String getUrl() {
		return path;
	}
}
