package it.com.atlassian.cpji.pages;

import com.atlassian.cpji.tests.pageobjects.SingleSelect;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
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
		elementFinder.find(By.id("targetEntityLink-single-select"), TimeoutType.SLOW_PAGE_LOAD).timed().isPresent().byDefaultTimeout();
		if(entitySelection.type(name).isSuggestionsOpen().by(5)) {
			entitySelection.clickSuggestion();
		}
		return this;
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

    public CopyDetailsPage next()
    {
        final String targetEntityLink = entitySelection.getValue();
        nextButton.click();
        return pageBinder.bind(CopyDetailsPage.class, issueId, targetEntityLink);
    }
}
