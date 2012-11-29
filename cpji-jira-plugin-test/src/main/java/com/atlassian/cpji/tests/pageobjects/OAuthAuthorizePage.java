package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
public class OAuthAuthorizePage implements Page {
	@javax.inject.Inject
	protected com.atlassian.pageobjects.PageBinder pageBinder;
	@javax.inject.Inject
	protected com.atlassian.pageobjects.elements.PageElementFinder elementFinder;
	@javax.inject.Inject
	protected com.atlassian.webdriver.AtlassianWebDriver driver;

	@ElementBy(id = "approve")
	private PageElement approve;

	@Override
	public String getUrl() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@WaitUntil
	public TimedCondition isAt() {
		return approve.timed().isVisible();
	}

	public JiraLoginPage approve() {
		approve.click();
		return pageBinder.bind(JiraLoginPage.class);
	}
}
