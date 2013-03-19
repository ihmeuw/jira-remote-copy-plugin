package it.com.atlassian.cpji

import org.junit._
import com.atlassian.jira.rest.client.domain._
import input.{IssueInput, LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import java.io.ByteArrayInputStream
import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstanceSuccessfulPage, CopyDetailsPage}
import org.junit.Assert._
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.cpji.tests.rules.CreateIssues
import org.joda.time.DateTime
import org.hamcrest.collection.IsIterableWithSize
import com.atlassian.cpji.tests.RawRestUtil._
import org.json.{JSONObject, JSONArray}
import com.atlassian.cpji.CopyIssueProcess
import com.atlassian.jira.rest.api.issue.RemoteIssueLinkCreateOrUpdateRequest
import collection.JavaConverters._

class TestCopySelectedElementsOfTheIssue extends AbstractCopyIssueTest with JiraObjects {

	var createIssues: CreateIssues = new CreateIssues(restClient1)
	var issue: Issue = null

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(jira1)

		issue = restClient1.getIssueClient
				.getIssue(restClient1.getIssueClient.createIssue(
			IssueInput.createWithFields(new FieldInput(SUMMARY_FIELD, "Issue with comments and attachments"),
				new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))), NPM).getKey,
			NPM)

		restClient1.getIssueClient.addComment(NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		restClient1.getIssueClient.addAttachment(NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)

		restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "NEL-1", "Duplicate"), NPM)
	}

	@After def tearDown() {
		try { restClient1.getIssueClient.removeIssue(issue.getKey, true, NPM) } catch { case e: Exception => "" }
	}

  def goToCopyDetails = CopyIssueProcess.goToCopyDetails(jira1, _ : java.lang.Long)

  @Test def testNotCopyingComments() {
		val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyComments.uncheck()

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey, NPM)
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](0))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](1))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(2, remoteLinks.length())
	}

	@Test def testNotCopyingAttachments() {
    	val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyAttachments.uncheck()

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey, NPM)
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](1))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](0))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(2, remoteLinks.length())
	}

	@Test def testNotCopyingLinks() {
    val copyDetailsPage: CopyDetailsPage = goToCopyDetails(issue.getId)

		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachmentsGroup.timed().isVisible)
		Poller.waitUntilTrue(copyDetailsPage.getCopyAttachments.timed.isEnabled)
		copyDetailsPage.getCopyIssueLinks.uncheck()

		val issueToInstancePage: CopyIssueToInstanceSuccessfulPage = copyDetailsPage.next().copyIssue()
		assertTrue(issueToInstancePage.isSuccessful)

		val issueRest: Issue = restClient2.getIssueClient.getIssue(issueToInstancePage.getRemoteIssueKey, NPM)
		assertEquals("CLONE - Issue with comments and attachments", issueRest.getSummary)
		assertThat(issueRest.getComments, IsIterableWithSize.iterableWithSize[Comment](1))
		assertThat(issueRest.getAttachments, IsIterableWithSize.iterableWithSize[Attachment](1))
		assertThat(issueRest.getIssueLinks, IsIterableWithSize.iterableWithSize[IssueLink](0))
		val remoteLinks: JSONArray = getIssueRemoteLinksJson(jira2, issueRest.getKey)
		assertEquals(1, remoteLinks.length())
	}


  @Test def shouldNotCopyClonerIssueLinks() {
    restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, "TST-1", "Cloners"), NPM)
    restClient1.getIssueClient.linkIssue(new LinkIssuesInput("TST-2", issue.getKey, "Cloners"), NPM)
    assertEquals(3, restClient1.getIssueClient.getIssue(issue.getKey, NPM).getIssueLinks.asScala.size)

    val result = goToCopyDetails(issue.getId).next().copyIssue()
    assertTrue(result.isSuccessful)

    val copiedIssue = restClient2.getIssueClient.getIssue(result.getRemoteIssueKey, NPM)
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
