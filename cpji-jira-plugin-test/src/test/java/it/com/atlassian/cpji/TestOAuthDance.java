package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.ScreenshotUtil;
import com.atlassian.cpji.tests.pageobjects.OAuthAuthorizePage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.cpji.tests.pageobjects.admin.ListApplicationLinksPage;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.binder.PageBindingWaitException;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.pageobjects.elements.query.Conditions.or;
import static org.junit.Assert.assertFalse;

/**
 * @since v3.0
 */
public class TestOAuthDance extends AbstractCopyIssueTest {
    private static final Logger logger = LoggerFactory.getLogger(TestOAuthDance.class);

	@Before
	public void setup() {
		login(jira1);
	}

	@Test
	public void doTheDanceBaby() throws Exception {
		final String issueKey = "TST-2";
		final Long issueId = 10100L;
		final String applicationId = "db60eb28-51aa-3f22-b3cc-b8967fa6281b";

        try {
            ListApplicationLinksPage appLinks = jira1.visit(ListApplicationLinksPage.class);

            if (appLinks.isAddApplicationLinkPresent()) { // method for adding application links changed in JIRA 6.1
                appLinks.clickAddApplicationLink().setApplicationUrl("http://localhost:2992/jira").next()
                        .setUsername(JiraLoginPage.USER_ADMIN).setPassword(JiraLoginPage.PASSWORD_ADMIN).next()
                        .setUseDifferentUsers().next();
            } else {
                try {
                    appLinks.setApplicationUrl("http://localhost:2992/jira").clickContinue()
                            .loginAsSystemAdminAndFollowRedirect(ListApplicationLinksPage.ConfirmApplicationUrlDialog.class)
                            .clickContinue().loginAsSystemAdminAndFollowRedirect(ListApplicationLinksPage.class);
                } catch (PageBindingWaitException e) {
                    if (e.getCause() instanceof UnhandledAlertException) {
                        // sometimes we get a dirty warning here
                        throw new RuntimeException("There was a modal window: " + jira1.getTester().getDriver().switchTo().alert().getText(), e);
                    }
                    throw e;
                }
            }

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
            selectTargetProjectPage.clickOAuthApproval(applicationId);
            OAuthAuthorizePage oAuthAuthorizePage;

            Poller.waitUntilTrue(or(isAt(OAuthAuthorizePage.class), isAt(JiraLoginPage.class)));
            if (isAt(OAuthAuthorizePage.class).now()) {
                oAuthAuthorizePage = jira1.getPageBinder().bind(OAuthAuthorizePage.class);
            } else {
                oAuthAuthorizePage = jira1.getPageBinder().bind(JiraLoginPage.class).loginAsSystemAdminAndFollowRedirect(
                        OAuthAuthorizePage.class);
            }
            oAuthAuthorizePage.approve();

            Poller.waitUntilTrue(or(isAt(SelectTargetProjectPage.class, selectTargetProjectPage.getIssueId()), isAt(JiraLoginPage.class)));
            if (isAt(JiraLoginPage.class).now()) {
                selectTargetProjectPage = jira1.getPageBinder().bind(JiraLoginPage.class).loginAsSystemAdminAndFollowRedirect(SelectTargetProjectPage.class, selectTargetProjectPage.getIssueId());
            } else {
                selectTargetProjectPage = jira1.getPageBinder().bind(SelectTargetProjectPage.class, selectTargetProjectPage.getIssueId());
            }
            assertFalse(selectTargetProjectPage.hasOAuthApproval(applicationId));
        } catch (Exception e) {
            ScreenshotUtil.attemptScreenshot(jira1.getTester().getDriver().getDriver(), "TestOAuthDance-failed");
            throw e;
        } finally {
            try {
                ListApplicationLinksPage page = jira1.visit(ListApplicationLinksPage.class);
                ListApplicationLinksPage.DeleteDialog deleteDialog = page.clickDelete("JIRA3");
                deleteDialog = deleteDialog.delete();
                deleteDialog.deleteAndReturn();
            } catch (Exception e) {
                logger.error("Unable to delete Application Link", e);
			}
		}
	}

    private TimedCondition isAt(Class<? extends AbstractJiraPage> pageClass, Object ... args) {
        return jira1.getPageBinder().delayedBind(pageClass, args).inject().get().isAt();
    }
}
