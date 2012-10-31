package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
    static JiraTestedProduct jira1 = TestedProductFactory.create(FirstJiraTestedProduct.class);
    static JiraTestedProduct jira2 = TestedProductFactory.create(SecondJiraTestedProduct.class);
}
