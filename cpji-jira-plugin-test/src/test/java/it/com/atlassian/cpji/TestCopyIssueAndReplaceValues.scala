package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstanceConfirmationPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit._
import collection.JavaConversions._
import com.atlassian.pageobjects.elements.query.Poller
import org.scalatest.junit.ShouldMatchersForJUnit
import com.atlassian.jira.rest.client.domain.input.{ComponentInput, VersionInput, IssueInputBuilder}
import com.atlassian.jira.rest.client.domain.Issue


class TestCopyIssueAndReplaceValues extends AbstractCopyIssueTest with JiraObjects with ShouldMatchersForJUnit {
	@Before def setUp {
//		testkit1.restoreDataFromResource("xml/jira1-backup.zip")
//		login(jira1)
	}

	@After def tearDown {
//		testkit1.restoreDataFromResource("xml/jira1-backup.zip")
	}

	@Test def testFillMissingValuesAndCopy {
//		restClient1.getVersionRestClient.createVersion(VersionInput.create("NEL", "version-1", null, null, false, false), NPM)
//		restClient1.getComponentClient.createComponent("NEL", new ComponentInput("component-1", null, null, null), NPM)
//
//		restClient1.getVersionRestClient.createVersion(VersionInput.create("TST", "other-version-1", null, null, false, false), NPM)
//		restClient1.getComponentClient.createComponent("TST", new ComponentInput("other-component-1", null, null, null), NPM)
//
//		val issueBuilder = new IssueInputBuilder("NEL", 3L, "Sample issue assigned to not mappable user")
//				.setAffectedVersionsNames(asJavaIterable(Array("version-1"))).setComponentsNames(asJavaIterable(Array("component-1")))
//
//		val issue: Issue = restClient1.getIssueClient
//				.getIssue(restClient1.getIssueClient.createIssue(issueBuilder.build(), NPM).getKey, NPM)
//
//		val selectTargetProjectPage: SelectTargetProjectPage = jira1
//				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(issue.getId))
//
//		selectTargetProjectPage.setDestinationProject("Test")
//		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
//		var permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next
//
//		Poller.waitUntilFalse(permissionChecksPage.areAllIssueFieldsRetained)
//		Poller.waitUntilTrue(permissionChecksPage.areAllRequiredFieldsFilledIn)
//
//		permissionChecksPage.setAffectsVersions(asJavaIterable(Array("other-version-1")))
//		permissionChecksPage.setComponents(asJavaIterable(Array("other-component-1")))
//
//		val copied = permissionChecksPage.copyIssue()
//		val issueKey = copied.getRemoteIssueKey
//		val restIssue = restClient1.getIssueClient.getIssue(issueKey, NPM)
//
//		val components = iterableAsScalaIterable(restIssue.getComponents)
//		components should have size (1)
//
//		var versions = iterableAsScalaIterable(restIssue.getAffectedVersions)
//		versions should have size (1)
	}

}
