package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyDetailsPage, CopyIssueToInstanceConfirmationPage, SelectTargetProjectPage}
import com.atlassian.jira.pageobjects.JiraTestedProduct
import com.atlassian.jira.pageobjects.setup.JiraWebTestRules
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.api.domain.input.{ComponentInput, IssueInputBuilder, VersionInput}
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.pageobjects.{DefaultProductInstance, TestedProductFactory}
import org.junit._

import scala.collection.JavaConversions._


object TestCopyIssueAndReplaceValues {
	val jira1: JiraTestedProduct = TestedProductFactory.create(classOf[JiraTestedProduct], new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null)

	@Rule val chain = JiraWebTestRules.forJira(jira1)
}

class TestCopyIssueAndReplaceValues extends JiraObjects with org.scalatest.Matchers with org.scalatest.junit.AssertionsForJUnit {

	@Before def setUp() {
		jira1.quickLoginAsAdmin()
	}

	@Test def testFillMissingValuesAndCopy() {
		val fromKey = "COPYFROM"
		val toKey = "COPYTO"

		testkit1.project().addProject("From project", fromKey, "admin")
		testkit1.project().addProject("To project", toKey, "admin")
		try {
			restClient1.getVersionRestClient.createVersion(VersionInput.create(fromKey, "version-1", null, null, false, false)).claim()
			restClient1.getComponentClient.createComponent(fromKey, new ComponentInput("component-1", null, null, null)).claim()

			restClient1.getVersionRestClient.createVersion(VersionInput.create(toKey, "other-version-1", null, null, false, false)).claim()
			restClient1.getComponentClient.createComponent(toKey, new ComponentInput("other-component-1", null, null, null)).claim()

			val issueBuilder = new IssueInputBuilder(fromKey, 3L, "Sample issue assigned to not mappable user")
					.setAffectedVersionsNames(asJavaIterable(Array("version-1")))
					.setComponentsNames(asJavaIterable(Array("component-1")))
					.setFixVersionsNames(asJavaIterable(Array("version-1")))

			val issue: Issue = restClient1.getIssueClient
					.getIssue(restClient1.getIssueClient.createIssue(issueBuilder.build()).claim().getKey).claim()

			val selectTargetProjectPage: SelectTargetProjectPage = jira1
					.visit(classOf[SelectTargetProjectPage], new java.lang.Long(issue.getId))

			selectTargetProjectPage.setDestinationProject("To project")
			val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
			var permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

			Poller.waitUntilFalse(permissionChecksPage.areAllIssueFieldsRetained)
			Poller.waitUntilTrue(permissionChecksPage.areAllRequiredFieldsFilledIn)

			permissionChecksPage.setAffectsVersions(asJavaIterable(Array("other-version-1")))
			permissionChecksPage.setFixVersions(asJavaIterable(Array("other-version-1")))
			permissionChecksPage.setComponents(asJavaIterable(Array("other-component-1")))

			val copied = permissionChecksPage.copyIssue()
			val issueKey = copied.getRemoteIssueKey
			val restIssue = restClient1.getIssueClient.getIssue(issueKey).claim()

			val components = iterableAsScalaIterable(restIssue.getComponents)
			components should have size (1)
			components.head.getName should be === "other-component-1"

			var versions = iterableAsScalaIterable(restIssue.getAffectedVersions)
			versions should have size (1)
			versions.head.getName should be === "other-version-1"

			var fixForVersions = iterableAsScalaIterable(restIssue.getFixVersions)
			fixForVersions should have size (1)
			fixForVersions.head.getName should be === "other-version-1"
		} finally {
			testkit1.project().deleteProject(fromKey)
			testkit1.project().deleteProject(toKey)
		}
	}

}
