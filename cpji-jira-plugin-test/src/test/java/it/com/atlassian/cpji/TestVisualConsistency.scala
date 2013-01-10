package it.com.atlassian.cpji

import org.junit.{Rule, Test}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.input.{LinkIssuesInput, ComplexIssueInputFieldValue, IssueInputBuilder}
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage
import com.atlassian.cpji.tests.ScreenshotUtil
import java.io.ByteArrayInputStream
import com.atlassian.jira.rest.client.domain.Comment
import org.joda.time.DateTime

class TestVisualConsistency extends AbstractCopyIssueTest with JiraObjects{

	@Rule def createIssues = new CreateIssues(restClient1)

	def takeScreenshot(message: String) {
		ScreenshotUtil.attemptScreenshot(jira1.getTester.getDriver.getDriver, "VisualConsistency - "+message)
	}

	@Test def testTakeScreenshotsForEveryStep(){

		testkit2.attachments().disable()

		try{

			login(jira1)
			val issueBuilder = new IssueInputBuilder("NEL", 3L, "Sample issue for screenshots").setAssigneeName("admin")
			val issue = createIssues.newIssue(issueBuilder.build())

			val issueToLinkBuilder = new IssueInputBuilder("NEL", 3L, "Sample issue for screenshots - I need one for linking")
			val issueToLinkWith = createIssues.newIssue(issueToLinkBuilder.build())

			restClient1.getIssueClient.addAttachment(NPM,
				issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)

			restClient1.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, issueToLinkWith.getKey, "Duplicate"), NPM)

			restClient1.getIssueClient.addComment(NPM, issue.getCommentsUri, new Comment(null, "Smile!", null, null, new DateTime, new DateTime, null, null))

			val selectTargetProject = jira1.visit(classOf[SelectTargetProjectPage], issue.getId)
			selectTargetProject.setDestinationProject("Destination not")
			takeScreenshot("01 Select target project")
			val details = selectTargetProject.next()
			takeScreenshot("02 Select target project")
			var confirm = details.next()
			takeScreenshot("03 Confirm values")
			confirm.copyIssue()
			takeScreenshot("04 Successful screen")
		} finally{
			testkit2.attachments().enable()
		}


	}

}
