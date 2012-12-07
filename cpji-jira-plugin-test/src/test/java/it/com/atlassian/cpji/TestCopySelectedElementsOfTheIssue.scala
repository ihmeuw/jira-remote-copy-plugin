package it.com.atlassian.cpji

import org.junit._
import com.atlassian.jira.rest.client.domain._
import input.{IssueInput, LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import java.io.ByteArrayInputStream
import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstancePage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.Assert._
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.cpji.tests.rules.CreateIssues
import org.joda.time.DateTime
import org.hamcrest.collection.IsIterableWithSize

class TestCopySelectedElementsOfTheIssue extends AbstractCopyIssueTest {

	var createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)
	var issue: Issue = null

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)

		issue = AbstractCopyIssueTest.restClient1.getIssueClient
				.getIssue(AbstractCopyIssueTest.restClient1.getIssueClient.createIssue(
			IssueInput.createWithFields(new FieldInput(SUMMARY_FIELD, "Issue with comments and attachments"),
				new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))), AbstractCopyIssueTest.NPM).getKey,
			AbstractCopyIssueTest.NPM)

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(AbstractCopyIssueTest.NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)

		AbstractCopyIssueTest.restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), AbstractCopyIssueTest.NPM)
	}

	@After def tearDown() {
		try { AbstractCopyIssueTest.restClient1.getIssueClient.removeIssue(issue.getKey, true, AbstractCopyIssueTest.NPM) } catch { case e: Exception => "" }
	}

	@Test def testNotCopyingComments() {
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
	}

	@Test def testNotCopyingAttachments() {
		val selectTargetProjectPage: SelectTargetProjectPage = AbstractCopyIssueTest.jira1.visit(classOf[SelectTargetProjectPage], issue.getId)
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next()

		assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyAttachments.uncheck()

		val issueToInstancePage: CopyIssueToInstancePage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = AbstractCopyIssueTest.restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey, AbstractCopyIssueTest.NPM)
		assertEquals(issueRest.getSummary, "Issue with comments and attachments")
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](1))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](0))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
	}
}
