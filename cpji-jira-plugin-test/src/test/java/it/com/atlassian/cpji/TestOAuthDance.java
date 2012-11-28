package it.com.atlassian.cpji;

import it.com.atlassian.cpji.pages.OAuthAuthorizePage;
import it.com.atlassian.cpji.pages.SelectTargetProjectPage;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
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
		viewIssue(jira1, issueKey);
		SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, issueId);
		selectTargetProjectPage = selectTargetProjectPage.clickOAuthApproval("db60eb28-51aa-3f22-b3cc-b8967fa6281b").loginAsSystemAdminAndFollowRedirect(
				OAuthAuthorizePage.class, selectTargetProjectPage.getIssueId()).approve();
	}
}
