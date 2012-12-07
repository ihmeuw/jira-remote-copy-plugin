package it.com.atlassian.cpji

import org.junit.{Before, Rule, Test}
import com.atlassian.jira.rest.client.domain._
import com.atlassian.jira.rest.client.domain.input.{LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import java.io.ByteArrayInputStream
import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstancePage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.Assert._
import com.atlassian.pageobjects.elements.query.Poller
import com.google.common.collect.Collections2
import com.atlassian.pageobjects.elements.Options
import org.hamcrest.core.IsCollectionContaining
import com.atlassian.cpji.tests.rules.CreateIssues
import org.joda.time.DateTime
import org.hamcrest.collection.IsIterableWithSize

class TestCopySelectedElementsOfTheIssue extends AbstractCopyIssueTest {

	var createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)
	}

	@Test def testNotCopyingComments() {
		val issue: Issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments and attachments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(AbstractCopyIssueTest.NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)

		try {
			AbstractCopyIssueTest.testkit1.issueLinking().createIssueLinkType("Duplicate", "duplicates", "is duplicated by")
			AbstractCopyIssueTest.restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), AbstractCopyIssueTest.NPM)

			val selectTargetProjectPage: SelectTargetProjectPage = AbstractCopyIssueTest.jira1.visit(classOf[SelectTargetProjectPage], issue.getId)
			val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next()

			assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
			copyDetailsPage.getCopyComments.uncheck()

			val issueToInstancePage: CopyIssueToInstancePage = copyDetailsPage.next().copyIssue()
			assertTrue(issueToInstancePage.isSuccessful)

			val issueRest: Issue = AbstractCopyIssueTest.restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey, AbstractCopyIssueTest.NPM)
			assertEquals(issueRest.getSummary, "Issue with comments and attachments")
			assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](0))
			assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](1))
			assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		} finally {
			try { AbstractCopyIssueTest.testkit1.issueLinking().deleteIssueLinkType("Duplicate") } catch { case e: Exception => "" }
		}
	}
}
