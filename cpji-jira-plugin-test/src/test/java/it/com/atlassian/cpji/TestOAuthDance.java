package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.OAuthAuthorizePage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.cpji.tests.pageobjects.admin.ListApplicationLinksPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.tests.annotations.JiraBuildNumberDependent;
import com.atlassian.jira.tests.annotations.LongCondition;
import com.atlassian.jira.tests.rules.JiraBuildNumberRule;
import com.atlassian.pageobjects.binder.PageBindingWaitException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;

/**
 * @since v3.0
 */
public class TestOAuthDance extends AbstractCopyIssueTest {
    private static final Logger logger = LoggerFactory.getLogger(TestOAuthDance.class);

    private static final int BN_JIRA_6_1 = 6100;

    @Rule
    public JiraBuildNumberRule jiraBuildNumberRule = new JiraBuildNumberRule(jira1);

	@Before
	public void setup() {
		login(jira1);
	}

    @JiraBuildNumberDependent(value = TestOAuthDance.BN_JIRA_6_1, condition = LongCondition.LESS_THAN)
	@Test
	public void doTheDanceBaby() {
		final String issueKey = "TST-2";
		final Long issueId = 10100L;
		final String applicationId = "db60eb28-51aa-3f22-b3cc-b8967fa6281b";

		try {
			ListApplicationLinksPage appLinks = jira1.visit(ListApplicationLinksPage.class);

			appLinks = appLinks.clickAddApplicationLink().setApplicationUrl("http://localhost:2992/jira").next()
					.setUsername(JiraLoginPage.USER_ADMIN).setPassword(JiraLoginPage.PASSWORD_ADMIN).next()
					.setUseDifferentUsers().next();

			try {
				viewIssue(jira1, issueKey);
			} catch (PageBindingWaitException e) {
				if (e.getCause() instanceof UnhandledAlertException) {
					// sometimes we get a dirty warning here
					jira1.getTester().getDriver().switchTo().alert().dismiss();
					viewIssue(jira1, issueKey);
				} else {
					throw e;
				}
			}

			SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, issueId);

			// usually you'd not have to log in when returning to first JIRA but since JIRA is usually accessed using localhost
			// it may redirect to the real hostname after OAuth dance
			selectTargetProjectPage = selectTargetProjectPage.clickOAuthApproval(
					applicationId).loginAsSystemAdminAndFollowRedirect(
					OAuthAuthorizePage.class).approve().loginAsSystemAdminAndFollowRedirect(SelectTargetProjectPage.class,
					selectTargetProjectPage.getIssueId());
			assertFalse(selectTargetProjectPage.hasOAuthApproval(applicationId));
		} finally {


			try {
                ListApplicationLinksPage page = jira1.visit(ListApplicationLinksPage.class);
                ListApplicationLinksPage.DeleteDialog deleteDialog = page.clickDelete("http://localhost:2992/jira");
                deleteDialog = deleteDialog.delete();
                deleteDialog.deleteAndReturn();
			} catch (Exception e) {
				logger.error("Unable to delete Application Link", e);
			}
		}
	}
}
