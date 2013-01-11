package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.BackdoorFactory;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.rules.EmptySystemDashboardRule;
import com.atlassian.jira.testkit.client.rules.WebSudoRule;
import com.atlassian.jira.tests.pageobjects.DefaultProductInstance;
import com.atlassian.jira.tests.rules.DirtyWarningTerminatorRule;
import com.atlassian.jira.tests.rules.MaximizeWindow;
import com.atlassian.jira.tests.rules.WebDriverScreenshot;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.testing.rule.SessionCleanupRule;
import org.junit.ClassRule;
import org.junit.Rule;

import java.net.URI;

/**
 * @since v2.1
 */
public abstract class AbstractCopyIssueTest
{
	final static NullProgressMonitor NPM = new NullProgressMonitor();

    static JiraTestedProduct jira1 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null);
    static JiraTestedProduct jira2 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2991/jira", "jira2", 2991, "/jira"), null);
    static JiraTestedProduct jira3 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2992/jira", "jira3", 2992, "/jira"), null);

	static Backdoor testkit1 = BackdoorFactory.getBackdoor(jira1);
	static Backdoor testkit2 = BackdoorFactory.getBackdoor(jira2);
	static Backdoor testkit3 = BackdoorFactory.getBackdoor(jira3);

	@ClassRule
	public static EmptySystemDashboardRule emptySystemDashboardRule = new EmptySystemDashboardRule(testkit1, testkit2, testkit3);

	@Rule
	public static SessionCleanupRule sessionCleanupRule = new SessionCleanupRule();
	@Rule
	public static MaximizeWindow maximizeWindow = new MaximizeWindow();
	@Rule
	public WebSudoRule webSudoRule = new WebSudoRule(testkit1, testkit2, testkit3);
	@Rule
	public static WebDriverScreenshot webDriverScreenshot = new WebDriverScreenshot();
	@Rule
	public static DirtyWarningTerminatorRule dirtyWarningTerminatorRule = new DirtyWarningTerminatorRule();

	static JiraRestClient restClient1 = getJiraRestClient(jira1);
	static JiraRestClient restClient2 = getJiraRestClient(jira2);
	static JiraRestClient restClient3 = getJiraRestClient(jira3);

	private static JiraRestClient getJiraRestClient(JiraTestedProduct jira1) {
		return new JerseyJiraRestClientFactory().createWithBasicHttpAuthentication(
				URI.create(jira1.getProductInstance().getBaseUrl()),
				JiraLoginPage.USER_ADMIN, JiraLoginPage.PASSWORD_ADMIN);
	}

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
