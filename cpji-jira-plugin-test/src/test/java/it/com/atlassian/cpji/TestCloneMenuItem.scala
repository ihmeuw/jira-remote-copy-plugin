package it.com.atlassian.cpji

import org.junit.{Ignore, Rule, Test}
import org.junit.Assert._
import org.hamcrest.collection.{IsIterableContainingInOrder, IsIterableWithSize}
import com.atlassian.cpji.tests.pageobjects._
import admin.ListApplicationLinksPage
import com.atlassian.pageobjects.elements.query.{TimedQuery, Poller}
import com.atlassian.jira.security.Permissions
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.IssueFieldId
import com.atlassian.jira.rest.client.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import java.lang.String
import BackdoorHelpers._
import org.hamcrest.core.StringContains
import org.hamcrest.{Description, BaseMatcher, Matchers, Matcher}
import com.atlassian.jira.pageobjects.navigator.{AdvancedSearch, BasicSearch}
import com.atlassian.cpji.tests.ScreenshotUtil
import com.atlassian.pageobjects.binder.PageBindingException
import org.apache.log4j.Logger

/**
 * Check if Clone/Copy menu item is visible by conditions described at https://jdog.atlassian.net/browse/JRADEV-16762
 *
 * View issue page would display "Remote Copy" menu item if
 * user is logged in,
 * he is in the allowed groups (or the list of allowed groups is empty)
 * and (he has permission to create issues for at least one local project or at least one JIRA application link is defined)
 *
 * Check [[it.com.atlassian.cpji.TestAllowedGroups]] for a test for allowed groups.
 */
class TestCloneMenuItem extends AbstractCopyIssueTest {
	val logger = Logger.getLogger(classOf[TestCloneMenuItem])

	val createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient3)

	@Rule def createIssuesRule = createIssues

	val jira1 = AbstractCopyIssueTest.jira1
	val jira3 = AbstractCopyIssueTest.jira3
	val testkit1 = AbstractCopyIssueTest.testkit1
	val testkit2 = AbstractCopyIssueTest.testkit2
	val testkit3 = AbstractCopyIssueTest.testkit3

	@Ignore @Test def shouldNotDisplayDefaultCloneActionWhenPluginIsInstalled() {
		login(jira1)
		val adminPage: ConfigureCopyIssuesAdminActionPage = jira1.visit(classOf[ConfigureCopyIssuesAdminActionPage], "TST")
		assertThat(adminPage.getAllowedGroups, IsIterableWithSize.iterableWithSize[String](0))
		val viewIssue: ExtendedViewIssuePage = AbstractCopyIssueTest.jira1.visit(classOf[ExtendedViewIssuePage], "TST-1")
		Poller.waitUntilFalse(viewIssue.getIssueActionsFragment.hasDefaultCloneAction)
		Poller.waitUntilTrue(viewIssue.getIssueActionsFragment.hasRICCloneAction)
	}

	@Ignore @Test def shouldNotDisplayDefaultCloneActionAtDOTWindow() {
		login(jira1)
		val viewIssue: ExtendedViewIssuePage = AbstractCopyIssueTest.jira1.visit(classOf[ExtendedViewIssuePage], "TST-1")
		var dialog = viewIssue.openDOTSection()
		val actionLinks = dialog.getActionsLinksByQuery("clone")
		assertThat(actionLinks, Matchers.contains(Matchers.containsString("SelectTargetProjectAction")));
		assertThat(actionLinks, Matchers.not(Matchers.contains(Matchers.containsString("CloneIssueDetails"))));
	}

	@Ignore @Test def shouldNotDisplayDefaultCloneActionAtIssueNavigator() {
		login(jira1)
		AbstractCopyIssueTest.jira1.visit(classOf[CommonBasicSearch])

		val actionsMenu = jira1.getPageBinder.bind(classOf[ExtendedIssueActionsMenu], java.lang.Long.valueOf(10100L))
		actionsMenu.open()


		val links = actionsMenu.getActionLinks()

		Poller.waitUntil(
			links.asInstanceOf[TimedQuery[Object]],
			Matchers.hasItem(Matchers.containsString("SelectTargetProjectAction")).asInstanceOf[Matcher[Object]]
		)


		Poller.waitUntil(
			links.asInstanceOf[TimedQuery[Object]],
			Matchers.not(Matchers.hasItem(Matchers.containsString("CloneIssueDetails"))).asInstanceOf[Matcher[Object]]
		)

	}

	@Test def shouldNotDisplayLinkIfUserIsNotLoggedIn() {
		val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "AN-1")
		Poller.waitUntilFalse(issuePage.getIssueActionsFragment.hasRICCloneAction)
	}

	@Test def shouldNotDisplayIfUserHasNoPermissionToCreateIssuesAndThereAreNoApplicationLinks() {
		val issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "AFER")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))
		try {
			testkit3.permissionSchemes().removeProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
			login(jira3)


			val issuePage: ExtendedViewIssuePage = jira3.visit(classOf[ExtendedViewIssuePage], issue.getKey)
			issuePage.getMoreActionsMenu.open()
			Poller.waitUntilFalse(issuePage.getIssueActionsFragment.hasRICCloneAction)
		} catch{
			case e: Exception => {
				logger.error("Troubles during checking permissions", e)
			}
		} finally {
			testkit3.permissionSchemes().addProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
			AbstractCopyIssueTest.restClient3.getIssueClient.removeIssue(issue.getKey, true, AbstractCopyIssueTest.NPM)
		}
	}

	@Test def shouldDisplayIfUserHasNoPermissionToCreateIssueButApplicationLinkExists() {
		try {
			removeProjectRolePermission(testkit1, 0, Permissions.CREATE_ISSUE, 10000)
			login(jira1)
			val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "TST-1")
			Poller.waitUntilTrue(issuePage.getIssueActionsFragment.hasRICCloneAction)
		} finally {
			addProjectRolePermission(testkit1, 0, Permissions.CREATE_ISSUE, 10000)
		}
	}

	@Test def shouldShowAnErrorWhenUserHasNoPermissionToCreateIssuesInRemoteApplications() {
		try {
			removeProjectRolePermission(testkit1, 10001, Permissions.CREATE_ISSUE, 10000)
			removeProjectRolePermission(testkit1, 0, Permissions.CREATE_ISSUE, 10000)
			removeProjectRolePermission(testkit2, 0, Permissions.CREATE_ISSUE, 10000)

			login(jira1)
			val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "TST-1")
			Poller.waitUntilTrue(issuePage.getIssueActionsFragment.hasRICCloneAction)
			issuePage.invokeRIC()
			val selectTargetProjectPage = jira1.getPageBinder.bind(classOf[SelectTargetProjectPage], java.lang.Long.valueOf(10000L))
			Poller.waitUntilTrue(selectTargetProjectPage.getTargetEntityWarningMessage.timed().isPresent)
		} finally {
			addProjectRolePermission(testkit2, 0, Permissions.CREATE_ISSUE, 10000)
			addProjectRolePermission(testkit1, 0, Permissions.CREATE_ISSUE, 10000)
			addProjectRolePermission(testkit1, 10001, Permissions.CREATE_ISSUE, 10000)
		}
	}

}
