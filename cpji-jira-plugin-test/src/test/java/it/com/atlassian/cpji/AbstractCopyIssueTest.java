package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.webtest.webdriver.setup.JiraWebTestRules;
import com.atlassian.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import org.junit.Rule;
import org.junit.rules.TestRule;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
    static JiraTestedProduct jira1 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null);
    static JiraTestedProduct jira2 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2991/jira", "jira2", 2991, "/jira"), null);

	@Rule
	public TestRule webTestRule1 = JiraWebTestRules.forJira(jira1);
	@Rule
	public TestRule webTestRule2 = JiraWebTestRules.forJira(jira2);
}
