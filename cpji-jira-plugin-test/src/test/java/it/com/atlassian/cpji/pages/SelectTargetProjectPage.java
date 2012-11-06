package it.com.atlassian.cpji.pages;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
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
    
    @ElementBy(id = "targetEntityLink")
    private SelectElement entitySelection;

    public SelectTargetProjectPage(final Long issueId)
    {
        this.issueId = issueId;
        this.url = String.format(URI_TEMPLATE, issueId);
    }

	public SelectTargetProjectPage setDestinationProject(@Nonnull String name) {
		entitySelection.select(Options.text(name));
		return this;
	}

    @Override
    public TimedCondition isAt()
    {
        return elementFinder.find(By.id("targetEntityLink-field")).timed().isPresent();
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
