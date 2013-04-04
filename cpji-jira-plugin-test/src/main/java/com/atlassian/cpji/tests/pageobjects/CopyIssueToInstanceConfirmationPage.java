package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.cpji.tests.pageobjects.confirmationPage.MappingResult;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.TimedElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.google.common.base.Preconditions;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import java.util.Iterator;

/**
 * Step 3 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class CopyIssueToInstanceConfirmationPage extends AbstractJiraPage
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

    public TimedQuery<Boolean> areAllIssueFieldsRetained(){
        return Conditions.not(elementFinder.find(By.id("fields-cannot-be-copied-header")).timed().isVisible());
    }

    public TimedQuery<Boolean> areAllRequiredFieldsFilledIn()
    {
        return Conditions.not(elementFinder.find(By.id("destination-fields-missing-header")).timed().isVisible());
    }

    public MappingResult getMappingResultFor(String field){
        return pageBinder.bind(MappingResult.class, field);
    }


    public CopyIssueToInstanceSuccessfulPage copyIssue()
    {
        copyIssueButton.click();
        return pageBinder.bind(CopyIssueToInstanceSuccessfulPage.class);
    }

	public TimedElement getFirstFieldGroup() {
		return elementFinder.find(By.cssSelector(".field-group")).timed();
	}

	public Iterator<PageElement> getFieldGroups() {
		return elementFinder.findAll(By.cssSelector(".field-group")).iterator();
	}

	public CopyIssueToInstanceConfirmationPage submitWithErrors() {
		copyIssueButton.click();
		return pageBinder.bind(CopyIssueToInstanceConfirmationPage.class);
	}

}
