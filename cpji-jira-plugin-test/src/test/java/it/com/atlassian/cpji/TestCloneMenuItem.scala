package it.com.atlassian.cpji

import org.junit.{Test, Before}
import org.junit.Assert._
import org.openqa.selenium.{WebElement, By}
import org.hamcrest.collection.IsIterableWithSize

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

	val jira1 = AbstractCopyIssueTest.jira1
	val jira3 = AbstractCopyIssueTest.jira3
	val testkit1 = AbstractCopyIssueTest.testkit1
	val testkit2 = AbstractCopyIssueTest.testkit2

	@Test def shouldNotDisplayLinkIfUserIsNotLoggedIn() {
		viewIssue(jira1, "AN-1")
		assertThat(jira1.getTester.getDriver.findElements(By.id(new RemoteCopyOperation().id)),
			IsIterableWithSize.iterableWithSize[WebElement](0))
	}

	@Test def shouldNotDisplayIfUserHasNoPermissionToCreateIssuesAndThereAreNoApplicationLinks() {
		login(jira1)
	}

	@Test def shouldDisplayIfUserHasNoPermissionToCreateIssueButApplicationLinkExists() {

	}

	@Test def shouldShowAnErrorWhenUserHasNoPermissionToCreateIssuesInRemoteApplications() {

	}

}
