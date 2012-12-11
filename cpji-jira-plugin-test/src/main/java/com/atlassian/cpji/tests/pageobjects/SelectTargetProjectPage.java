package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
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
    private static final String URI_TEMPLATE = "/secure/SelectTargetProjectAction!default.jspa?id=%d";

    private final Long issueId;
    private final String url;

    @ElementBy(className = "submit")
    private PageElement nextButton;

	@ElementBy(id = "targetEntityLink-container")
	private PageElement targetEntityLinkContainer;

    @ElementBy(className = "warning", within = "targetEntityLinkContainer")
    private PageElement targetEntityErrorMessage;
    
    private SingleSelect entitySelection;

    public SelectTargetProjectPage(final Long issueId)
    {
        this.issueId = issueId;
        this.url = String.format(URI_TEMPLATE, issueId);
    }

	@SuppressWarnings("unused")
	@Init
	public void initialize() {
		entitySelection = pageBinder.bind(SingleSelect.class, targetEntityLinkContainer);
	}

	public SelectTargetProjectPage setDestinationProject(@Nonnull String name) {
        waitForDestinationProjectField();
        if(entitySelection.type(name).isSuggestionsOpen().by(5)) {
			entitySelection.clickSuggestion();
		}
		return this;
	}

    public void waitForDestinationProjectField()
    {
        elementFinder.find(By.id("targetEntityLink-single-select"), TimeoutType.SLOW_PAGE_LOAD).timed().isPresent().byDefaultTimeout();
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

	public boolean hasOAuthApproval(String applicationId) {
		Preconditions.checkNotNull(applicationId);
		waitForDestinationProjectField();
		return !elementFinder.findAll(By.cssSelector(String.format("a[data-application-id=%s]", applicationId))).isEmpty();
	}

	public JiraLoginPage clickOAuthApproval(String applicationId) {
		Preconditions.checkNotNull(applicationId);
		By link = By.cssSelector(String.format("a[data-application-id=%s]", applicationId));
		driver.waitUntilElementIsVisible(link);
		elementFinder.find(link).click();
		return pageBinder.bind(JiraLoginPage.class);
	}

    public CopyDetailsPage next()
    {
        waitForDestinationProjectField();
        final String targetEntityLink = entitySelection.getValue();
        nextButton.click();
        return pageBinder.bind(CopyDetailsPage.class, issueId, targetEntityLink);
    }

    public String getTargetEntityWarningMessage(){
        return targetEntityErrorMessage.timed().getText().byDefaultTimeout();
    }

	@Nonnull
	public Long getIssueId() {
		return issueId;
	}
}
