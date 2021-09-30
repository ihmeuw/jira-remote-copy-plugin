package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.webdriver.utils.element.WebDriverPoller;

/**
 * @since v3.0
 */
public class OAuthAuthorizePage extends AbstractJiraPage {
	@javax.inject.Inject
	protected com.atlassian.pageobjects.PageBinder pageBinder;
	@javax.inject.Inject
	protected com.atlassian.pageobjects.elements.PageElementFinder elementFinder;
	@javax.inject.Inject
	protected org.openqa.selenium.WebDriver driver;
	@javax.inject.Inject
	protected WebDriverPoller webDriverPoller;

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

	public void approve() {
		approve.click();
	}
}
