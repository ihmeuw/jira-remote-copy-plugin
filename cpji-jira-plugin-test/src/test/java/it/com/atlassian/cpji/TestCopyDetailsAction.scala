package it.com.atlassian.cpji

import org.junit.{Before, Rule, Test}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{Comment, IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import org.joda.time.DateTime
import com.atlassian.cpji.tests.pageobjects.Options
import org.junit.Assert._
import java.io.ByteArrayInputStream
import org.hamcrest.core.IsCollectionContaining
import com.google.common.collect.Collections2
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.cpji.CopyIssueProcess
;

class TestCopyDetailsAction extends AbstractCopyIssueTest {
	val createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)
	}

	def goToCopyDetails = CopyIssueProcess.goToCopyDetails(AbstractCopyIssueTest.jira1, _: java.lang.Long)

	@Test def testAdvancedSectionIncludesItemsBasedOnIssueContent() {
		val issue: Issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			assertFalse(copyDetailsPage.isCopyCommentsGroupVisible)
			assertFalse(copyDetailsPage.isCopyAttachmentsGroupVisible)
			assertFalse(copyDetailsPage.isCopyIssueLinksGroupVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(AbstractCopyIssueTest.NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			assertTrue(copyDetailsPage.isCopyCommentsGroupVisible)
			assertFalse(copyDetailsPage.isCopyAttachmentsGroupVisible)
			assertFalse(copyDetailsPage.isCopyIssueLinksGroupVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}


		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName)

		{
			val copyDetailsPage = goToCopyDetails(issue.getId)

			assertTrue(copyDetailsPage.isCopyCommentsGroupVisible)
			assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
			assertFalse(copyDetailsPage.isCopyIssueLinksGroupVisible)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
		}

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), AbstractCopyIssueTest.NPM)

		val copyDetailsPage = goToCopyDetails(issue.getId)

		assertTrue(copyDetailsPage.isCopyCommentsGroupVisible)
		assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
		assertTrue(copyDetailsPage.isCopyIssueLinksGroupVisible)
		assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
	}


	@Test def testAdvancedSectionReportsMissingFeaturesOnRemoteSide() {
		val issue: Issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName)

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), AbstractCopyIssueTest.NPM)

		try {
			AbstractCopyIssueTest.testkit2.attachments().disable()
			AbstractCopyIssueTest.testkit2.issueLinking().disable()

			val copyDetailsPage = goToCopyDetails(issue.getId)

			assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyAttachments.timed.isEnabled)
			assertTrue("Copy Issue Link section should be visible when remote JIRA has link disabled (so we can show the meaningful message)",
				copyDetailsPage.isCopyIssueLinksGroupVisible)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinks.timed.isEnabled)
			assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCreateIssueLinks.timed.isPresent)
			assertThat(Collections2.transform(copyDetailsPage.getCreateIssueLinks.getAllOptions, Options.getText),
				IsCollectionContaining.hasItems("From original to copy", "None"))
		} finally {
			AbstractCopyIssueTest.testkit2.attachments().enable()
			AbstractCopyIssueTest.testkit2.issueLinking().enable()
		}
	}

}
