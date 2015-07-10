package it.com.atlassian.cpji

import java.io.ByteArrayInputStream

import com.atlassian.cpji.CopyIssueProcess
import com.atlassian.cpji.action.RemoteIssueLinkType
import com.atlassian.cpji.tests.RawRestUtil._
import com.atlassian.cpji.tests.pageobjects.{CopyDetailsPage, CopyIssueToInstanceSuccessfulPage}
import com.atlassian.pageobjects.elements.query.Poller
import org.hamcrest.collection.IsIterableWithSize
import org.joda.time.DateTime
import org.json.{JSONArray, JSONObject}
import org.junit.Assert._
import org.junit._
import com.atlassian.jira.rest.client.api.domain.input.{ComplexIssueInputFieldValue, FieldInput, IssueInput, LinkIssuesInput}
import com.atlassian.jira.rest.client.api.domain._
import scala.collection.JavaConverters._

class TestCopySelectedElementsOfTheIssue extends AbstractCopyIssueTest with JiraObjects {

	var issue: Issue = null

	@Before def setUp() {
		login(jira1)

		issue = restClient1.getIssueClient
				.getIssue(restClient1.getIssueClient.createIssue(
			IssueInput.createWithFields(new FieldInput(IssueFieldId.SUMMARY_FIELD, "Issue with comments and attachments"),
				new FieldInput(IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))).claim()
				.getKey).claim()

		restClient1.getIssueClient.addComment(issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null)).claim()

		restClient1.getIssueClient.addAttachment(
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
			this.getClass.getCanonicalName).claim()

		restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate")).claim()
	}

	def goToCopyDetails = CopyIssueProcess.goToCopyDetails(jira1, _: java.lang.Long)

	@Test def testNotCopyingComments() {
		val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyComments.uncheck()

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey).claim()
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](0))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](1))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(2, remoteLinks.length())
	}

	@Test def testNotCopyingAttachmentsAndCreateLinkOnlyFromCopiedToOriginal() {
		val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyAttachments.uncheck()
		copyDetailsPage.setCreateIssueLink(RemoteIssueLinkType.INCOMING)

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey).claim()
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](1))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](0))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(2, remoteLinks.length())
		compareRemoteLink("clones", issue.getKey, remoteLinks.getJSONObject(1))

		val links = getIssueRemoteLinksJson(jira1, issue.getKey)
		assertEquals(0, links.length())
	}

	@Test def testNotCopyingLinksAndCreateLinkOnlyFromOriginal() {
		val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyIssueLinks.uncheck()
		copyDetailsPage.setCreateIssueLink(RemoteIssueLinkType.OUTGOING)

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey).claim()
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](1))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](1))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(0, remoteLinks.length())

		val link = getIssueRemoteLinksJson(jira1, issue.getKey).getJSONObject(0)
		compareRemoteLink("is cloned by", issueRest.getKey, link)
	}


	@Test def shouldNotCopyClonerIssueLinks() {
		restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "TST-1", "Cloners")).claim()
		restClient1.getIssueClient.linkIssue(new LinkIssuesInput("TST-2", issue.getKey, "Cloners")).claim()
		assertEquals(3, restClient1.getIssueClient.getIssue(issue.getKey).claim().getIssueLinks.asScala.size)

		val result = goToCopyDetails(issue.getId).next().copyIssue()
		assertTrue(result.isSuccessful)

		val copiedIssue = restClient2.getIssueClient.getIssue(result.getRemoteIssueKey).claim()
		//issue should have two remote issue links - old "duplicate" and fresh new created cloned
		val remoteLinks = getIssueRemoteLinksJson(jira2, copiedIssue.getKey)
		assertEquals(2, remoteLinks.length())
		compareRemoteLink("duplicates", "NEL-1", remoteLinks.getJSONObject(0))
		compareRemoteLink("clones", issue.getKey, remoteLinks.getJSONObject(1))
	}


	private def compareRemoteLink(relationship: String, title: String, json: JSONObject) {
		assertEquals(relationship, json.getString("relationship"))
		assertEquals(title, json.getJSONObject("object").getString("title"))
	}

}
