package it.com.atlassian.cpji;

import com.atlassian.pageobjects.page.HomePage;
import org.junit.Test;

/**
 *
 */
public class TestCopyIssue extends AbstractCopyIssueTest
{
    @Test
    public void testSomething()
    {
        // Just a dummy test for now, as a proof of concept
        System.out.println("Logged in? " + PRODUCT.visit(HomePage.class).getHeader().isLoggedIn());
    }
}
