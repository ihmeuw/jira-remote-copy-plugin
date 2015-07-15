package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyDetailsPage, CopyIssueToInstanceConfirmationPage, SelectTargetProjectPage}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.pageobjects.pages.DashboardPage
import com.atlassian.jira.rest.client.api.domain.IssueFieldId._
import com.atlassian.jira.rest.client.api.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.api.domain.{BasicUser, IssueFieldId}
import com.atlassian.pageobjects.elements.query.Poller
import org.hamcrest.Matchers
import org.hamcrest.collection.IsIterableContainingInOrder
import org.junit.Assert._
import org.junit.{Before, Rule, Test}
import org.openqa.selenium.By

import scala.collection.JavaConversions._

class TestWorkingAsFred extends AbstractCopyIssueTest with JiraObjects {
	val createIssues: CreateIssues = new CreateIssues(restClient1)

	@Rule def createIssuesRule = createIssues

	@Before def setUp() {
		jira1.gotoLoginPage().login("fred", "fred", classOf[DashboardPage])
	}

	@Test def shouldCopyWhenUserDoesntHaveModifyReporterPermission() {
		val selectTargetProjectPage: SelectTargetProjectPage = jira1
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10200L))
		selectTargetProjectPage.setDestinationProject("Blah")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.areAllIssueFieldsRetained)
		Poller.waitUntilFalse(permissionChecksPage.areAllRequiredFieldsFilledIn)

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		assertThat(asJavaIterable(permissionChecksPage.getFieldGroups
				.map(element => element.find(By.tagName("label")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String]("Assignee", "Reporter"))

		val successfulPage = permissionChecksPage.copyIssue()
		assertTrue(successfulPage.isSuccessful)

		val issue = restClient2.getIssueClient.getIssue(successfulPage.getRemoteIssueKey).claim()
		assertThat(issue.getAssignee.getName, Matchers.equalTo("admin"))
		assertThat(issue.getReporter.getName, Matchers.equalTo("fred"))
	}

	@Test def shouldCopyIssueWithoutAssignee() {
		testkit1.applicationProperties().setOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED, true)
		testkit1.project().setProjectDefaultAssignee(10100l, false)
		try {
			val newIssue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue without assignee"),
				new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "NEL")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

			assertThat(newIssue.getAssignee, Matchers.nullValue(classOf[BasicUser]))

			val selectTargetProjectPage: SelectTargetProjectPage = jira1
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(newIssue.getId))

			selectTargetProjectPage.setDestinationProject("Blah")
			val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
			val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

			Poller.waitUntilTrue(permissionChecksPage.areAllIssueFieldsRetained)
			Poller.waitUntilFalse(permissionChecksPage.areAllRequiredFieldsFilledIn)

			Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
			assertThat(asJavaIterable(permissionChecksPage.getFieldGroups
				.map(element => element.find(By.tagName("label")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String]("Assignee", "Reporter"))

			val successfulPage = permissionChecksPage.copyIssue()
			assertTrue(successfulPage.isSuccessful)

			val issue = restClient2.getIssueClient.getIssue(successfulPage.getRemoteIssueKey).claim()
			assertThat(issue.getAssignee.getName, Matchers.equalTo("admin"))
			assertThat(issue.getReporter.getName, Matchers.equalTo("fred"))
		} finally {
			testkit1.applicationProperties().setOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED, true)
			testkit1.project().setProjectDefaultAssignee(10100l, true)
		}
	}


}
