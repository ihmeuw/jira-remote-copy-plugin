package it.com.atlassian.cpji

import java.io.ByteArrayInputStream

import com.atlassian.cpji.CopyIssueProcess
import com.atlassian.cpji.tests.pageobjects.CopyDetailsPage
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.rest.client.api.domain.input.{ComplexIssueInputFieldValue, FieldInput, IssueInput, LinkIssuesInput}
import com.atlassian.jira.rest.client.api.domain.{Comment, Issue, IssueFieldId}
import com.atlassian.jira.security.Permissions
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.pageobjects.elements.{CheckboxElement, PageElement}
import it.com.atlassian.cpji.BackdoorHelpers._
import org.apache.commons.lang.StringUtils
import org.hamcrest.Matchers
import org.joda.time.DateTime
import org.junit._

class TestCopyRespectsRemotePermissions extends AbstractCopyIssueTest {

	var createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)
	var issue: Issue = null

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)

		issue = AbstractCopyIssueTest.restClient1.getIssueClient
				.getIssue(AbstractCopyIssueTest.restClient1.getIssueClient.createIssue(
			IssueInput.createWithFields(new FieldInput(IssueFieldId.SUMMARY_FIELD, "Issue with comments and attachments"),
				new FieldInput(IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))).claim().getKey).claim()

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName).claim()

		AbstractCopyIssueTest.restClient1.getIssueClient
				.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate")).claim()
	}

	@After def tearDown() {
		try {
			AbstractCopyIssueTest.restClient1.getIssueClient.deleteIssue(issue.getKey, true).claim()
		} catch {
			case e: Exception =>
		}
	}

	def goToCopyDetails = CopyIssueProcess.goToCopyDetails(AbstractCopyIssueTest.jira1, _: java.lang.Long)

	@Test def shouldDisableCopyCommentsCheckboxIfDoesntHavePermissionToCommentIssues() {
		try {
			removeProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.COMMENT_ISSUE, 10000)

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)
			isPresentAndDisabled(copyDetailsPage, _.getCopyCommentsGroup, _.getCopyComments, _.getCopyCommentsNotice)

		} finally {
			addProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.COMMENT_ISSUE, 10000)
		}
	}

	@Test def shouldDisableCopyLinksCheckboxIfDoesntHavePermissionToLinkIssues() {
		val roleIds = removeProjectRolePermissions(AbstractCopyIssueTest.testkit2, 0, Permissions.LINK_ISSUE, Array(10000, 10001))
		try {

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)
			isPresentAndDisabled(copyDetailsPage, _.getCopyIssueLinksGroup, _.getCopyIssueLinks, _.getCopyIssueLinksNotice)

		} finally {
			addProjectRolePermissions(AbstractCopyIssueTest.testkit2, 0, Permissions.LINK_ISSUE, roleIds)
		}
	}

	@Test def shouldDisableCopyAttachmentsCheckboxIfDoesntHavePermissionToCreateAttachments() {
		try {
			removeProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.CREATE_ATTACHMENT, 10000)

			val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)
			isAttachmentsPresentAndDisabled(copyDetailsPage)
		} finally {
			addProjectRolePermission(AbstractCopyIssueTest.testkit2, 0, Permissions.CREATE_ATTACHMENT, 10000)
		}
	}

	@Test def shouldDisplayWarningMessageWhenSomeAttachmentsExceedMaxSize() {
		var apControl = AbstractCopyIssueTest.testkit2.applicationProperties()
		val attachmentSize =  apControl.getString(APKeys.JIRA_ATTACHMENT_SIZE)
		try{
			apControl.setString(APKeys.JIRA_ATTACHMENT_SIZE, 30L.toString)

      //single attachment
      AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(
        issue.getAttachmentsUri, new ByteArrayInputStream(StringUtils.repeat("this is a stream", 100).getBytes("UTF-8")),
        "attachment1.txt").claim()

      var detailsPage = goToCopyDetails(issue.getId)
      isAttachmentsPresentAndEnabled(detailsPage)
      Poller.waitUntil(detailsPage.getCopyAttachmentsNotice.timed().getText, Matchers.containsString("ATTACHMENT1.TXT exceeds maximum attachment size "))


      AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(
        issue.getAttachmentsUri, new ByteArrayInputStream(StringUtils.repeat("this is a stream", 100).getBytes("UTF-8")),
        "attachment2.txt").claim()

      detailsPage = goToCopyDetails(issue.getId)
      isAttachmentsPresentAndEnabled(detailsPage)
      Poller.waitUntil(detailsPage.getCopyAttachmentsNotice.timed().getText, Matchers.containsString("2 ATTACHMENTS exceed maximum attachment size "))

      detailsPage.showNotCopiedAttachmentsList()
      Poller.waitUntil(detailsPage.getNotCopiedAttachmentsList, Matchers.containsString("attachment1.txt"))
      Poller.waitUntil(detailsPage.getNotCopiedAttachmentsList, Matchers.containsString("attachment2.txt"))

    } finally {
			apControl.setString(APKeys.JIRA_ATTACHMENT_SIZE, attachmentSize)
		}
	}

	@Test def shouldDisableCopyAttachmentsCheckboxWhenAllAttachmentsExceedMaxSize(){
		var apControl = AbstractCopyIssueTest.testkit2.applicationProperties()
		val attachmentSize =  apControl.getString(APKeys.JIRA_ATTACHMENT_SIZE)
		try{
			apControl.setString(APKeys.JIRA_ATTACHMENT_SIZE, 1L.toString)
			var detailsPage = goToCopyDetails(issue.getId)
			isAttachmentsPresentAndDisabled(detailsPage)
		} finally {
			apControl.setString(APKeys.JIRA_ATTACHMENT_SIZE, attachmentSize)
		}
	}

	private def isAttachmentsPresentAndDisabled = isPresentAndDisabled(_ : CopyDetailsPage, _.getCopyAttachmentsGroup, _.getCopyAttachments, _.getCopyAttachmentsNotice)
	private def isAttachmentsPresentAndEnabled = isPresentAndEnabled(_ : CopyDetailsPage, _.getCopyAttachmentsGroup, _.getCopyAttachments, _.getCopyAttachmentsNotice)

	private def isPresentAndEnabled(obj:CopyDetailsPage, group: CopyDetailsPage => PageElement, checkbox: CopyDetailsPage=>CheckboxElement, notice : CopyDetailsPage => PageElement) = {
		Poller.waitUntilTrue(group(obj).timed().isVisible)
		Poller.waitUntilTrue(checkbox(obj).timed().isPresent)
		Poller.waitUntilTrue(checkbox(obj).timed().isEnabled)
		Poller.waitUntilTrue(notice(obj).timed.isVisible)
	}


	private def isPresentAndDisabled(obj:CopyDetailsPage, group: CopyDetailsPage => PageElement, checkbox: CopyDetailsPage=>CheckboxElement, notice : CopyDetailsPage => PageElement) = {
		Poller.waitUntilTrue(group(obj).timed().isVisible)
		Poller.waitUntilTrue(checkbox(obj).timed().isPresent)
		Poller.waitUntilFalse(checkbox(obj).timed().isEnabled)
		Poller.waitUntilTrue(notice(obj).timed.isVisible)
	}

}
