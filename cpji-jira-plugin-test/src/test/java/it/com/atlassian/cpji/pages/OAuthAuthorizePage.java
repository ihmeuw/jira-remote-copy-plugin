package it.com.atlassian.cpji.pages;

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

	private final Long issueId;

	@ElementBy(id = "approve")
	private PageElement approve;

	public OAuthAuthorizePage(Long issueId) {
		this.issueId = issueId;
	}

	@Override
	public String getUrl() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@WaitUntil
	public TimedCondition isAt() {
		return approve.timed().isVisible();
	}

	public SelectTargetProjectPage approve() {
		approve.click();
		return pageBinder.bind(SelectTargetProjectPage.class, issueId);
	}
}
