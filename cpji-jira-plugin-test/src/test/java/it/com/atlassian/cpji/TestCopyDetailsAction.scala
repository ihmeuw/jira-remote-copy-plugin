package it.com.atlassian.cpji

import java.io.ByteArrayInputStream

import com.atlassian.cpji.CopyIssueProcess
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.api.domain.{Issue, IssueFieldId, Comment}
import com.atlassian.jira.rest.client.api.domain.input.{LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.pageobjects.elements.query.Poller
import org.hamcrest.core.IsCollectionContaining
import org.joda.time.DateTime
import org.junit.Assert._
import org.junit.{Before, Rule, Test}

class TestCopyDetailsAction extends AbstractCopyIssueTest {
	val createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)
	}

	def goToCopyDetails = CopyIssueProcess.goToCopyDetails(AbstractCopyIssueTest.jira1, _: java.lang.Long)

	@Test def testAdvancedSectionIncludesItemsBasedOnIssueContent() {
		val issue: Issue = createIssues.newIssue(new FieldInput(IssueFieldId.SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilFalse(copyDetailsPage.getCopyCommentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null)).claim()

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyCommentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}


		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName).claim()

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyCommentsGroup.timed().isVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate")).claim()

		val copyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyCommentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
		assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)

		AbstractCopyIssueTest.restClient1.getIssueClient.deleteIssue(issue.getKey, true).claim()
	}


	@Test def testAdvancedSectionReportsMissingFeaturesOnRemoteSide() {
		val issue: Issue = createIssues.newIssue(new FieldInput(IssueFieldId.SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName).claim()

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate")).claim()

		try {
			AbstractCopyIssueTest.testkit2.attachments().disable()
			AbstractCopyIssueTest.testkit2.issueLinking().disable()

			val copyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyAttachments.timed.isEnabled)
			Poller.waitUntilTrue("Copy Issue Link section should be visible when remote JIRA has link disabled (so we can show the meaningful message)",
				copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinks.timed.isEnabled)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
			assertThat(copyDetailsPage.getCreateIssueLinks, IsCollectionContaining.hasItems("From original to copy", "None"))
		} finally {
			AbstractCopyIssueTest.testkit2.attachments().enable()
			AbstractCopyIssueTest.testkit2.issueLinking().enable()
			AbstractCopyIssueTest.restClient1.getIssueClient.deleteIssue(issue.getKey, true).claim()
		}
	}

}
