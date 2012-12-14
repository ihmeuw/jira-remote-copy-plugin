package it.com.atlassian.cpji

import org.junit._
import com.atlassian.jira.rest.client.domain._
import input.{IssueInput, LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import java.io.ByteArrayInputStream
import com.atlassian.cpji.tests.pageobjects.CopyDetailsPage
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.cpji.tests.rules.CreateIssues
import org.joda.time.DateTime
import com.atlassian.cpji.CopyIssueProcess
import it.com.atlassian.cpji.BackdoorHelpers._
import com.atlassian.jira.security.Permissions

class TestCopyRespectsRemotePermissions extends AbstractCopyIssueTest {

	var createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)
	var issue: Issue = null

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)

		issue = AbstractCopyIssueTest.restClient1.getIssueClient
				.getIssue(AbstractCopyIssueTest.restClient1.getIssueClient.createIssue(
			IssueInput.createWithFields(new FieldInput(SUMMARY_FIELD, "Issue with comments and attachments"),
				new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))),
			AbstractCopyIssueTest.NPM).getKey,
			AbstractCopyIssueTest.NPM)

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(AbstractCopyIssueTest.NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName)

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), AbstractCopyIssueTest.NPM)
	}

	@After def tearDown() {
		try {
			AbstractCopyIssueTest.restClient1.getIssueClient.removeIssue(issue.getKey, true, AbstractCopyIssueTest.NPM)
		} catch {
			case e: Exception => ""
		}
	}

	def goToCopyDetails = CopyIssueProcess.goToCopyDetails(AbstractCopyIssueTest.jira1, _: java.lang.Long)

	@Test def shouldDisableCopyCommentsCheckboxIfDoesntHavePermissionToCommentIssues() {
		try {
			removeProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.COMMENT_ISSUE, 10000)

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyCommentsGroup.timed().isVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCopyComments.timed().isPresent)
			Poller.waitUntilFalse(copyDetailsPage.getCopyComments.timed().isEnabled)
			Poller.waitUntilTrue(copyDetailsPage.getCopyCommentsNotice.timed.isVisible)
		} finally {
			addProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.COMMENT_ISSUE, 10000)
		}
	}

	@Test def shouldDisableCopyLinksCheckboxIfDoesntHavePermissionToLinkIssues() {
		try {
			removeProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.LINK_ISSUE, 10001)

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyIssueLinksGroup.timed().isVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCopyIssueLinks.timed().isPresent)
			Poller.waitUntilFalse(copyDetailsPage.getCopyIssueLinks.timed().isEnabled)
			Poller.waitUntilTrue(copyDetailsPage.getCopyIssueLinksNotice.timed.isVisible)
		} finally {
			addProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.LINK_ISSUE, 10001)
		}
	}

	@Test def shouldDisableCopyAttachmentsCheckboxIfDoesntHavePermissionToCreateAttachments() {
		try {
			removeProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.CREATE_ATTACHMENT, 10000)

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed().isPresent)
			Poller.waitUntilFalse(copyDetailsPage.getCopyAttachments.timed().isEnabled)
			Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsNotice.timed.isVisible)
		} finally {
			addProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.CREATE_ATTACHMENT, 10000)
		}
	}
}
