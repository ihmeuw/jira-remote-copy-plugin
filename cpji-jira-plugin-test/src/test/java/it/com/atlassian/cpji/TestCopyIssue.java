package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import it.com.atlassian.cpji.pages.CopyDetailsPage;
import it.com.atlassian.cpji.pages.CopyIssueToInstancePage;
import it.com.atlassian.cpji.pages.PermissionChecksPage;
import it.com.atlassian.cpji.pages.SelectTargetProjectPage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @since v2.1
 */
public class TestCopyIssue extends AbstractCopyIssueTest
{
    private static final String ISSUE_KEY = "TST-1";
    private static final Long ISSUE_ID = 10000L;

    @Before
    public void setUp()
    {
        login(jira1);
    }

    @Test
    public void testSimpleRemoteCopy()
    {
        viewIssue(jira1, ISSUE_KEY);
        SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, ISSUE_ID);
        
        final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
        final PermissionChecksPage permissionChecksPage = copyDetailsPage.next();

        assertTrue(permissionChecksPage.isAllSystemFieldsRetained());
        assertTrue(permissionChecksPage.isAllCustomFieldsRetained());

        final CopyIssueToInstancePage copyIssueToInstancePage = permissionChecksPage.copyIssue();
        assertTrue(copyIssueToInstancePage.isSuccessful());

        final String remoteIssueKey = copyIssueToInstancePage.getRemoteIssueKey();

        login(jira2);
        final ViewIssuePage viewIssuePage = viewIssue(jira2, remoteIssueKey);

        // TODO inspect contents of this page
    }

    private void login(final JiraTestedProduct jiraTestedProduct)
    {
        jiraTestedProduct.gotoLoginPage().loginAsSysAdmin(DashboardPage.class);
    }

    private ViewIssuePage viewIssue(final JiraTestedProduct jiraTestedProduct, final String issueKey)
    {
        return jiraTestedProduct.getPageBinder().navigateToAndBind(ViewIssuePage.class, issueKey);
    }
}
