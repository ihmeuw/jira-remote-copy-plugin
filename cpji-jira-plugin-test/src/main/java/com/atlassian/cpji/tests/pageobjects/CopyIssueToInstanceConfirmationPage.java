package com.atlassian.cpji.tests.pageobjects;

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

	public CopyIssueToInstanceConfirmationPage typeToTextField(String name, CharSequence... value) {
		Preconditions.checkNotNull(name);
		PageElement textField = elementFinder.find(By.name(name));
		textField.clear();
		if (value != null) {
			textField.type(value);
		}
		return this;
	}

	public CopyIssueToInstanceConfirmationPage setMultiSelect(String id, String... items) {
		Preconditions.checkNotNull(id);
		MultiSelectUtil.setMultiSelect(this.pageBinder, id, items);
		return this;
	}

	public SingleSelect getSingleSelect(String containerId) {
		Preconditions.checkNotNull(containerId);
		final PageElement selectContainer = elementFinder.find(By.id(containerId));
		return pageBinder.bind(SingleSelect.class, selectContainer);
	}

	public SelectElement getSelectElement(String name) {
		Preconditions.checkNotNull(name);
		return elementFinder.find(By.name(name), SelectElement.class);
	}
}
