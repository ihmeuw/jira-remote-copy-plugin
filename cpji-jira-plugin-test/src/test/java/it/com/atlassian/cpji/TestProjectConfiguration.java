package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.collect.Iterables;
import com.atlassian.cpji.tests.pageobjects.ConfigureCopyIssuesAdminActionPage;
import com.atlassian.cpji.tests.pageobjects.JiraLoginPageWithWarnings;
import com.atlassian.cpji.tests.pageobjects.PermissionViolationPage;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @since v2.1
 */
public class TestProjectConfiguration extends AbstractCopyIssueTest
{
    @Before
    public void setUp()
    {
        login(jira1);
    }

	@Test
	public void testProjectConfigurationDoesntIncludeSummary() {
		ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "NEL");
		assertThat(adminPage.getRequiredFields(), expectedRequiredFields());
	}

    @Test
    public void defaultReporterShouldReturnErrorOnInvalidUserAndSaveValidOne() {
        ConfigureCopyIssuesAdminActionPage adminPage = ConfigureCopyIssuesAdminActionPage.AsDialog.open(jira1,
				"NEL");

        adminPage.getDefaultValues().typeIntoReporterField("FAKE USER");
        adminPage = adminPage.submit();

        assertTrue(adminPage.hasGeneralErrorsMessage());
        assertThat(adminPage.getErrors(), IsIterableContainingInAnyOrder.containsInAnyOrder("The reporter specified is not a user."));

        adminPage.getDefaultValues().setReporter("fred");
        adminPage = adminPage.submit();

        assertThat(adminPage.getConfigChanges(), IsIterableContainingInAnyOrder.containsInAnyOrder("Saved default value for field 'Reporter'"));
        assertEquals("Fred Flinstone", adminPage.getDefaultValues().getReporterText());
    }

    @Test
    public void projectConfigurationShouldRememberReportersForDifferentIssueTypes(){
        ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "NEL");

        adminPage.getDefaultValues().changeIssueType("ta").setReporter("fred");
        adminPage.submit();

        adminPage.getDefaultValues().changeIssueType("bug").setReporter("admin");
        adminPage.submit();

        String reporter = adminPage.getDefaultValues().changeIssueType("ta").getReporterText();
        assertEquals("Fred Flinstone", reporter);

        reporter = adminPage.getDefaultValues().changeIssueType("bug").getReporterText();
        assertEquals("admin", reporter);

    }

    private Matcher<Iterable<? extends String>> expectedRequiredFields() {
        return IsIterableContainingInAnyOrder.<String>containsInAnyOrder(startsWith("Issue Type"), startsWith("Reporter"));
    }

	@Test
	public void permissionDeniedShouldBePresentedWhenAnonymous() {
		jira1.logout();
		jira1.getTester().getDriver().navigate().to(jira1.getProductInstance().getBaseUrl() + "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=TST");
		jira1.getPageBinder().bind(JiraLoginPage.class);
		final JiraLoginPageWithWarnings jiraPage = jira1.getPageBinder().bind(JiraLoginPageWithWarnings.class);
		assertTrue(jiraPage.hasWarnings());
		final PageElement warning = Iterables.getFirst(jiraPage.getWarnings(), null);
		assertNotNull(warning);
		assertEquals("You must log in to access this page.", warning.find(By.tagName("p")).getText());
	}

	@Test
	public void permissionDeniedWhenRegularUser() {
		jira1.logout();
		jira1.gotoLoginPage().login("fred", "fred", PermissionViolationPage.class, "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=TST");
	}
}
