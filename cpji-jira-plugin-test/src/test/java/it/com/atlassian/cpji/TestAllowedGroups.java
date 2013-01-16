package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.ConfigureCopyIssuesAdminActionPage;
import com.atlassian.cpji.tests.pageobjects.ExtendedViewIssuePage;
import com.atlassian.cpji.tests.pageobjects.PermissionViolationPage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;

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
	public void allowedGroupsAreRemembered() {
		jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(ImmutableList.of("jira-administrators")).submit();
		try {
			assertThat(jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST").getAllowedGroups(),
				IsIterableContainingInAnyOrder.containsInAnyOrder("jira-administrators"));
		} finally {
			try { jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(
					null).submit(); } catch(Exception e) { log.error(e.getMessage()); }
		}
	}

	@Test
	public void testAllUsersAllowed() {
		ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST");
		assertThat(adminPage.getAllowedGroups(), IsIterableWithSize.<String>iterableWithSize(0));

		for(String user : ImmutableList.of("fred", "admin")) {
			jira1.logout();
			final ExtendedViewIssuePage issuePage = jira1.gotoLoginPage().login(user, user, ExtendedViewIssuePage.class, "TST-1");
            issuePage.invokeRIC();
			jira1.getPageBinder().bind(SelectTargetProjectPage.class, 10000L);
		}
	}

	@Test
	public void testOnlyJiraAdminsAllowed() {
		jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(ImmutableList.of("jira-administrators")).submit();
		try {
			for(String user : ImmutableList.of("fred")) {
				jira1.logout();
                final ExtendedViewIssuePage issuePage = jira1.gotoLoginPage().login(user, user, ExtendedViewIssuePage.class, "TST-1");
                Poller.waitUntilFalse(issuePage.getIssueActionsFragment().hasRICCloneAction());
                jira1.visit(PermissionViolationPage.class, SelectTargetProjectPage.buildUrl(10000L));
				jira1.visit(PermissionViolationPage.class, "CopyDetailsAction.jspa?id=10000&targetEntityLink=8835b6b9-5676-3de4-ad59-bbe987416662|TST");
				final String atl_token = (String) jira1.getTester().getDriver().executeScript("return atl_token()");
				jira1.visit(PermissionViolationPage.class, "CopyIssueToInstanceAction.jspa?key=TST-1&atl_token=" + atl_token);
			}

			for(String user : ImmutableList.of("admin")) {
				jira1.logout();
				final ExtendedViewIssuePage issuePage = jira1.gotoLoginPage().login(user, user, ExtendedViewIssuePage.class, "TST-1");
                issuePage.invokeRIC();
				jira1.getPageBinder().bind(SelectTargetProjectPage.class, 10000L);
			}
		} finally {
			try { jira1.gotoLoginPage().loginAsSysAdmin(ConfigureCopyIssuesAdminActionPage.class, "TST").setAllowedGroups(
					null).submit(); } catch(Exception e) { log.error(e.getMessage()); }
		}
	}
}
