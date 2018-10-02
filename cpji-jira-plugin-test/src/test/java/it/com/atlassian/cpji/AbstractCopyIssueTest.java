package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.BackdoorFactory;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.config.junit4.rule.DirtyWarningTerminatorRule;
import com.atlassian.jira.pageobjects.config.junit4.rule.RuleChainBuilder;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.pageobjects.setup.JiraWebDriverScreenshotRule;
import com.atlassian.jira.pageobjects.setup.JiraWebTestRules;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.rules.WebSudoRule;
import com.atlassian.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.testing.rule.SessionCleanupRule;
import com.atlassian.webdriver.testing.rule.WindowSizeRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.net.URI;
import javax.annotation.Nonnull;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
    static JiraTestedProduct jira1 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null);
    static JiraTestedProduct jira2 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2991/jira", "jira2", 2991, "/jira"), null);
    static JiraTestedProduct jira3 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2992/jira", "jira3", 2992, "/jira"), null);

	static Backdoor testkit1 = BackdoorFactory.getBackdoor(jira1);
	static Backdoor testkit2 = BackdoorFactory.getBackdoor(jira2);
	static Backdoor testkit3 = BackdoorFactory.getBackdoor(jira3);

	@Rule
	public RuleChain jiraRules = JiraWebTestRules.forJira(jira1);

	static JiraRestClient restClient1 = getJiraRestClient(jira1);
	static JiraRestClient restClient2 = getJiraRestClient(jira2);
	static JiraRestClient restClient3 = getJiraRestClient(jira3);

	@Nonnull
	public static JiraRestClient getJiraRestClient(JiraTestedProduct jira1) {
		return new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
				URI.create(jira1.getProductInstance().getBaseUrl()),
				JiraLoginPage.USER_ADMIN, JiraLoginPage.PASSWORD_ADMIN);
	}

	protected void login(final JiraTestedProduct jiraTestedProduct)
	{
		jiraTestedProduct.quickLoginAsSysadmin();
	}

	protected <M extends com.atlassian.pageobjects.Page> M login(final JiraTestedProduct jiraTestedProduct, final Class<M> clazz, Object ... args)
	{
		return jiraTestedProduct.quickLoginAsSysadmin(clazz, args);
	}

	protected ViewIssuePage viewIssue(final JiraTestedProduct jiraTestedProduct, final String issueKey)
	{
		return jiraTestedProduct.getPageBinder().navigateToAndBind(ViewIssuePage.class, issueKey);
	}
}
