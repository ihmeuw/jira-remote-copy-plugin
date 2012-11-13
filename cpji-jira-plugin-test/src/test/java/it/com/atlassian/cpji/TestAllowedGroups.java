package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.google.common.collect.ImmutableList;
import it.com.atlassian.cpji.pages.ConfigureCopyIssuesAdminActionPage;
import it.com.atlassian.cpji.pages.PermissionViolationPage;
import it.com.atlassian.cpji.pages.SelectTargetProjectPage;
import org.apache.log4j.Logger;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertThat;

/**
 * @since v2.1
 */
public class TestAllowedGroups extends AbstractCopyIssueTest {

	private final Logger log = Logger.getLogger(this.getClass());

	@Before
	public void setup() {
		login(jira1);
	}

	@Test
	public void testAllUsersAllowed() {
		ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST");
		assertThat(adminPage.getAllowedGroups(), IsIterableWithSize.<String>iterableWithSize(0));

		for(String user : ImmutableList.of("fred", "admin")) {
			jira1.logout();
			final ViewIssuePage issuePage = jira1.gotoLoginPage().login(user, user, ViewIssuePage.class, "TST-1");
			issuePage.getIssueMenu().invoke(new RemoteCopyOperation());
			jira1.getPageBinder().bind(SelectTargetProjectPage.class, 10000L);
		}
	}

	@Test
	public void testOnlyJiraAdminsAllowed() {
		jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(ImmutableList.of("jira-administrators")).submit();
		try {
			for(String user : ImmutableList.of("fred")) {
				jira1.logout();
				jira1.gotoLoginPage().login(user, user, ViewIssuePage.class, "TST-1");
				assertThat(jira1.getTester().getDriver().findElements(By.id(new RemoteCopyOperation().id())),
						IsIterableWithSize.<WebElement>iterableWithSize(0));
				jira1.visit(PermissionViolationPage.class, "SelectTargetProjectAction!default.jspa?key=TST-1");
				jira1.visit(PermissionViolationPage.class, "CopyDetailsAction.jspa?id=10000&targetEntityLink=8835b6b9-5676-3de4-ad59-bbe987416662|TST");
				final String atl_token = (String) jira1.getTester().getDriver().executeScript("return atl_token()");
				jira1.visit(PermissionViolationPage.class, "PermissionChecksAction.jspa?key=TST-1&atl_token=" + atl_token);
				jira1.visit(PermissionViolationPage.class, "CopyIssueToInstanceAction.jspa?key=TST-1&atl_token=" + atl_token);
			}

			for(String user : ImmutableList.of("admin")) {
				jira1.logout();
				final ViewIssuePage issuePage = jira1.gotoLoginPage().login(user, user, ViewIssuePage.class, "TST-1");
				issuePage.getIssueMenu().invoke(new RemoteCopyOperation());
				jira1.getPageBinder().bind(SelectTargetProjectPage.class, 10000L);
			}
		} finally {
			try { jira1.gotoLoginPage().loginAsSysAdmin(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(
					null).submit(); } catch(Exception e) { log.error(e.getMessage()); }
		}
	}
}
