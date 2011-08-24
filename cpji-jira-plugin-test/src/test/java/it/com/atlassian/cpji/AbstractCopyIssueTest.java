package it.com.atlassian.cpji;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;

/**
 *
 */
public abstract class AbstractCopyIssueTest
{
    static TestedProduct<?> PRODUCT = TestedProductFactory.create(
            System.getProperty("tested.app", JiraTestedProduct.class.getName()));
}
