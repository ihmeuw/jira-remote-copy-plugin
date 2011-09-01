package it.com.atlassian.cpji.pages;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Provides some helpful utility methods for dealing with Page Objects.
 *
 * @since v2.1
 */
public class PageObjectHelper
{
    private PageObjectHelper() {}

    public static boolean isMessagePresent(final String messageToFind, final PageElementFinder elementFinder)
    {
        final List<PageElement> messages = elementFinder.findAll(By.className("aui-message"));
        for (final PageElement message : messages)
        {
            if (messageToFind.equals(message.getText()))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean hasMessageStartingWith(final String startingWith, final PageElementFinder elementFinder)
    {
        return (getMessageStartingWith(startingWith, elementFinder) != null);
    }

    public static String getMessageStartingWith(final String startingWith, final PageElementFinder elementFinder)
    {
        final List<PageElement> messages = elementFinder.findAll(By.className("aui-message"));
        for (final PageElement message : messages)
        {
            if (message.getText().startsWith(startingWith))
            {
                return message.getText();
            }
        }

        return null;
    }
}
