package it.com.atlassian.cpji

import org.junit.{Rule, Test}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.api.domain.input.{IssueInputBuilder, LinkIssuesInput}
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage
import com.atlassian.cpji.tests.ScreenshotUtil
import java.io.ByteArrayInputStream

import com.atlassian.jira.rest.client.api.domain.Comment
import org.joda.time.DateTime

import scala.util.Random

class TestVisualConsistency extends AbstractCopyIssueTest with JiraObjects {

	@Rule def createIssues = new CreateIssues(restClient1)

	def takeScreenshot(message: String) {
		ScreenshotUtil.attemptScreenshot(jira1.getTester.getDriver.getDriver, "VisualConsistency - " + message)
	}

	@Test def testTakeScreenshotsForEveryStep() {

		testkit2.attachments().disable()

		val unmappedUser: String = "user_"+Random.alphanumeric.take(10).mkString("")
		try {

			login(jira1)
			val issueBuilder = new IssueInputBuilder("NEL", 3L, "Sample issue for screenshots").setAssigneeName("admin")
			val issue = createIssues.newIssue(issueBuilder.build())

			val issueToLinkBuilder = new IssueInputBuilder("NEL", 3L,
				"Sample issue for screenshots - I need one for linking")
			val issueToLinkWith = createIssues.newIssue(issueToLinkBuilder.build())

			restClient1.getIssueClient.addAttachment(
				issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")),
				this.getClass.getCanonicalName).claim()

			restClient1.getIssueClient
					.linkIssue(new LinkIssuesInput(issue.getKey, issueToLinkWith.getKey, "Duplicate")).claim()

			restClient1.getIssueClient.addComment(issue.getCommentsUri,
				new Comment(null, "Smile!", null, null, new DateTime, new DateTime, null, null)).claim()

			val selectTargetProject = jira1.visit(classOf[SelectTargetProjectPage], issue.getId)
			selectTargetProject.setDestinationProject("Destination not")
			takeScreenshot("01 Select target project")
			val details = selectTargetProject.next()
			takeScreenshot("02 Select target project")
			var confirm = details.next()
			takeScreenshot("03 Confirm values")
			confirm.copyIssue()
			takeScreenshot("04 Successful screen")

			//prepare for displaying incorrect data
			testkit1.usersAndGroups().addUser(unmappedUser, "rst", "Really Strange User", "really@strange.user")
			testkit1.usersAndGroups().addUserToGroup(unmappedUser, "jira-developers")
			testkit1.issues().assignIssue(issue.getKey, unmappedUser)

			jira1.visit(classOf[SelectTargetProjectPage], issue.getId).setDestinationProject("Some fields required")
					.next().next()
			takeScreenshot("03b Uneditable requilred fields - failure")


		} finally {
			testkit1.usersAndGroups().deleteUser(unmappedUser)
			testkit2.attachments().enable()
		}


	}

}
