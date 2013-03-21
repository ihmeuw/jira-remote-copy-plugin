package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstanceConfirmationPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.{Before, Test}
import org.junit.Assert._
import org.junit.Assert
import org.hamcrest.collection.IsIterableContainingInOrder
import org.openqa.selenium.By
import collection.JavaConverters._
import collection.JavaConversions._
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.pageobjects.elements.Options
import com.atlassian.jira.pageobjects.pages.DashboardPage
import org.hamcrest.Matchers

class TestWorkingAsFred extends AbstractCopyIssueTest with JiraObjects {
	@Before def setUp {
		jira1.gotoLoginPage().login("fred", "fred", classOf[DashboardPage])
	}

	@Test def shouldCopyWhenUserDoesntHaveModifyReporterPermission {
		val selectTargetProjectPage: SelectTargetProjectPage = jira1
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10200L))
		selectTargetProjectPage.setDestinationProject("Blah")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilFalse(permissionChecksPage.areAllIssueFieldsRetained)
		Poller.waitUntilFalse(permissionChecksPage.areAllRequiredFieldsFilledIn)

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		assertThat(asJavaIterable(permissionChecksPage.getFieldGroups()
				.map(element => element.find(By.tagName("label")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String]("Assignee", "Reporter"))

		val successfulPage = permissionChecksPage.copyIssue()
		assertTrue(successfulPage.isSuccessful)

		val issue = restClient2.getIssueClient.getIssue(successfulPage.getRemoteIssueKey, NPM)
		assertThat(issue.getAssignee.getName, Matchers.equalTo("admin"))
		assertThat(issue.getReporter.getName, Matchers.equalTo("fred"))
	}
}
