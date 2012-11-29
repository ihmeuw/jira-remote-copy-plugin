package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.admin.ListApplicationLinksPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.cpji.tests.pageobjects.OAuthAuthorizePage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @since v3.0
 */
public class TestOAuthDance extends AbstractCopyIssueTest {

	@Before
	public void setup() {
		login(jira1);
	}

	@Test
	public void doTheDanceBaby() {
		final String issueKey = "TST-2";
		final Long issueId = 10100L;
		final String applicationId = "db60eb28-51aa-3f22-b3cc-b8967fa6281b";

		try {
			ListApplicationLinksPage appLinks = jira1.visit(ListApplicationLinksPage.class);
			appLinks.clickAddApplicationLink().setApplicationUrl("http://localhost:2992/jira").next()
					.setUsername(JiraLoginPage.USER_ADMIN).setPassword(JiraLoginPage.PASSWORD_ADMIN).next()
					.setUseDifferentUsers().next();

			viewIssue(jira1, issueKey);
			SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, issueId);

			// usually you'd not have to log in when returning to first JIRA but since JIRA is usually accessed using localhost
			// it may redirect to the real hostname after OAuth dance
			selectTargetProjectPage = selectTargetProjectPage.clickOAuthApproval(
					applicationId).loginAsSystemAdminAndFollowRedirect(
					OAuthAuthorizePage.class).approve().loginAsSystemAdminAndFollowRedirect(SelectTargetProjectPage.class,
					selectTargetProjectPage.getIssueId());
			assertFalse(selectTargetProjectPage.hasOAuthApproval(applicationId));
		} finally {
			jira1.visit(ListApplicationLinksPage.class).clickDelete("http://localhost:2992/jira").delete().deleteAndReturn();
		}
	}
}
