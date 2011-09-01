package it.com.atlassian.cpji;

import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
    static JiraTestedProduct jira1 = TestedProductFactory.create(FirstJiraTestedProduct.class);
    static JiraTestedProduct jira2 = TestedProductFactory.create(SecondJiraTestedProduct.class);
}
