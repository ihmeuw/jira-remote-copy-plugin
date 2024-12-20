package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.components.fields.SingleSelect;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.webdriver.utils.element.ElementIsVisible;
import com.google.common.base.Preconditions;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;

/**
 * Step 1 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class SelectTargetProjectPage extends AbstractJiraPage
{
    private static final String URI_TEMPLATE = "/secure/SelectTargetProject!default.jspa?id=%d";

    private final Long issueId;
    private final String url;

    @ElementBy(className = "submit")
    protected PageElement nextButton;

	@ElementBy(id = "targetEntityLink-container")
	protected PageElement targetEntityLinkContainer;

    @ElementBy(className = "error", within = "targetEntityLinkContainer", timeoutType = TimeoutType.SLOW_PAGE_LOAD)
	protected PageElement targetEntityErrorMessage;

	@ElementBy (id = "targetEntityLink-single-select", timeoutType = TimeoutType.SLOW_PAGE_LOAD)
	protected PageElement targetEntitySingleSelect;

	private SingleSelect entitySelection;

    public SelectTargetProjectPage(final Long issueId)
    {
        this.issueId = issueId;
        this.url = buildUrl(issueId);
    }

	@SuppressWarnings("unused")
	@Init
	public void initialize() {

		entitySelection = pageBinder.bind(SingleSelect.class, targetEntityLinkContainer);
	}

	public SelectTargetProjectPage setDestinationProject(@Nonnull String name) {
        entitySelection.select(name);
		return this;
	}

    public PageElement getTargetEntitySingleSelect()
    {
        return targetEntitySingleSelect;
    }

    @Override
    public TimedCondition isAt()
    {
        return Conditions.forMatcher(elementFinder.find(By.className("current")).timed().getText(), Matchers
				.equalToIgnoringCase("Select project"));
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public static String buildUrl(Long issueId){
        return String.format(URI_TEMPLATE, issueId);
    }

	public boolean hasOAuthApproval(String applicationId) {
		Preconditions.checkNotNull(applicationId);
		Poller.waitUntilTrue(getTargetEntitySingleSelect().timed().isPresent());
		return !elementFinder.findAll(By.cssSelector(String.format("a[data-application-id=%s]", applicationId))).isEmpty();
	}

	public void clickOAuthApproval(String applicationId) {
		Preconditions.checkNotNull(applicationId);
		By link = By.cssSelector(String.format("a[data-application-id=%s]", applicationId));
		webDriverPoller.waitUntil(new ElementIsVisible(link));
		elementFinder.find(link).click();
	}

    public CopyDetailsPage next()
    {
        Poller.waitUntilTrue(getTargetEntitySingleSelect().timed().isPresent());
        final String targetEntityLink = entitySelection.getValue();
        nextButton.click();
        return pageBinder.bind(CopyDetailsPage.class, issueId, targetEntityLink);
    }

    public PageElement getTargetEntityWarningMessage() {
        return targetEntityErrorMessage;
    }

	@Nonnull
	public Long getIssueId() {
		return issueId;
	}
}
