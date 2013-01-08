package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.TimedElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import java.util.Iterator;

/**
 * Step 3 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class PermissionChecksPage extends AbstractJiraPage
{
    private static final String URL = "/secure/PermissionChecksAction.jspa";

    @ElementBy(className = "submit")
    private PageElement copyIssueButton;

    @Override
    public TimedCondition isAt()
    {
        return Conditions.forMatcher(elementFinder.find(By.className("current")).timed().getText(), Matchers.equalToIgnoringCase("Confirmation"));
    }

    @Override
    public String getUrl()
    {
        return URL;
    }

    public boolean isAllSystemFieldsRetained()
    {
        return isMessagePresent("All system field values will be retained");
    }

    public boolean isAllCustomFieldsRetained()
    {
        return isMessagePresent("All custom field values will be retained");
    }

    private boolean isMessagePresent(final String message)
    {
        return PageObjectHelper.isMessagePresent(message, elementFinder);
    }

    public CopyIssueToInstancePage copyIssue()
    {
        copyIssueButton.click();
        return pageBinder.bind(CopyIssueToInstancePage.class);
    }

	public TimedElement getFirstFieldGroup() {
		return elementFinder.find(By.cssSelector(".field-group")).timed();
	}

	public Iterator<PageElement> getFieldGroups() {
		return elementFinder.findAll(By.cssSelector(".field-group")).iterator();
	}

	public PermissionChecksPage submitWithErrors() {
		copyIssueButton.click();
		return pageBinder.bind(PermissionChecksPage.class);
	}
}
