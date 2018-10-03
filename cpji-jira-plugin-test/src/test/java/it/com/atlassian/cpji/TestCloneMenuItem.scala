package it.com.atlassian.cpji

import com.atlassian.cpji.tests.ScreenshotUtil
import com.atlassian.cpji.tests.pageobjects._
import com.atlassian.cpji.tests.pageobjects.admin.ListApplicationLinksPage
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.api.domain.IssueFieldId
import com.atlassian.jira.rest.client.api.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.security.Permissions
import com.atlassian.pageobjects.elements.query.Poller
import it.com.atlassian.cpji.BackdoorHelpers._
import org.apache.log4j.Logger
import org.junit.Assert._
import org.junit.{Rule, Test}

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
class TestCloneMenuItem extends AbstractCopyIssueTest with JiraObjects {
	val logger = Logger.getLogger(classOf[TestCloneMenuItem])

	val createIssues: CreateIssues = new CreateIssues(restClient3)

	@Rule def createIssuesRule = createIssues


	@Test def shouldNotDisplayLinkIfUserIsNotLoggedIn() {
		jira1.logout()
		val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "AN-1")
		assertFalse(issuePage.hasClone)

	}

	@Test def shouldNotDisplayIfUserHasNoPermissionToCreateIssuesAndThereAreNoApplicationLinks() {
		val issue = createIssues.newIssue(new FieldInput(IssueFieldId.SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "AFER")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))
		try {
			testkit3.permissionSchemes().removeProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
			login(jira3)

			jira3.visit(classOf[ListApplicationLinksPage])
			ScreenshotUtil.attemptScreenshot(jira3.getTester.getDriver.getDriver, "shouldNotDisplayIfUserHasNoPermissionToCreateIssuesAndThereAreNoApplicationLinks - applicationLinks")

			val issuePage: ExtendedViewIssuePage = jira3.visit(classOf[ExtendedViewIssuePage], issue.getKey)
			issuePage.getMoreActionsMenu.open()
			assertFalse(issuePage.hasClone)
		} catch{
			case e: Exception => {
				logger.error("Troubles during checking permissions", e)
			}
		} finally {
			testkit3.permissionSchemes().addProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
//			AbstractCopyIssueTest.restClient3.getIssueClient.deleteIssue(issue.getKey, true).claim()
		}
	}

	@Test def shouldDisplayIfUserHasNoPermissionToCreateIssueButApplicationLinkExists() {
		try {
			removeProjectRolePermission(testkit1, 0, Permissions.CREATE_ISSUE, 10000)
			login(jira1)
			val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "TST-1")
			assertTrue(issuePage.hasRIC)
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
			assertTrue(issuePage.hasRIC)
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
