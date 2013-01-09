package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

/**
 * Final step for Remote Copy, showing whether the operation was successful or not.
 *
 * @since v2.1
 */
public class CopyIssueToInstanceSuccessfulPage extends AbstractJiraPage
{
    private static final String URL = "/secure/CopyIssueToInstanceAction.jspa";

    @Override
    public TimedCondition isAt()
    {
        return Conditions.forMatcher(elementFinder.find(By.cssSelector(".done:last-child")).timed().getText(), Matchers.equalToIgnoringCase("Confirmation"));
    }

    @Override
    public String getUrl()
    {
        return URL;
    }

    public boolean isSuccessful()
    {
        return PageObjectHelper.hasMessageStartingWith("Copy successful", elementFinder);
    }

    public String getRemoteIssueKey()
    {
        final String message = PageObjectHelper.getMessageStartingWith("Copy successful", elementFinder);
        if (message == null)
        {
            return null;
        }

        final String search = "has been copied to ";
        return message.substring(message.indexOf(search) + search.length());
    }
}
