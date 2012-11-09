package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.rules.EnableDarkFeatureRule;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.webtest.webdriver.setup.JiraWebTestRules;
import com.atlassian.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
    static JiraTestedProduct jira1 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null);
    static JiraTestedProduct jira2 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2991/jira", "jira2", 2991, "/jira"), null);

	@ClassRule
	public static EnableDarkFeatureRule pldRule = new EnableDarkFeatureRule("com.atlassian.jira.config.PDL", jira1, jira2);

	@ClassRule
	public static EnableDarkFeatureRule commonHeaderRule = new EnableDarkFeatureRule("com.atlassian.jira.darkfeature.CommonHeader", jira1, jira2);


	@Rule
	public TestRule webTestRule1 = JiraWebTestRules.forJira(jira1);
	@Rule
	public TestRule webTestRule2 = JiraWebTestRules.forJira(jira2);

	protected void login(final JiraTestedProduct jiraTestedProduct)
	{
		jiraTestedProduct.gotoLoginPage().loginAsSysAdmin(DashboardPage.class);
	}

	protected <M extends com.atlassian.pageobjects.Page> M login(final JiraTestedProduct jiraTestedProduct, final Class<M> clazz, Object ... args)
	{
		return jiraTestedProduct.gotoLoginPage().loginAsSysAdmin(clazz, args);
	}

	protected ViewIssuePage viewIssue(final JiraTestedProduct jiraTestedProduct, final String issueKey)
	{
		return jiraTestedProduct.getPageBinder().navigateToAndBind(ViewIssuePage.class, issueKey);
	}

}
