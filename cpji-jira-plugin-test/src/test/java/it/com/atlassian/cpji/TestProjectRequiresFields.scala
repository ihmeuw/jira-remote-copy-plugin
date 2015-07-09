package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstanceConfirmationPage, CopyDetailsPage, SelectTargetProjectPage}
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder
import org.junit.{Rule, Before, Test, Assert}
import org.junit.Assert._
import org.hamcrest.collection.IsIterableContainingInOrder
import org.openqa.selenium.By
import collection.JavaConverters._
import collection.JavaConversions._
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.pageobjects.elements.Options
import org.scalatest.junit.ShouldMatchersForJUnit
import org.codehaus.jettison.json.JSONArray
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.cpji.tests.pageobjects.confirmationPage.MappingResult
import org.hamcrest.Matchers
import org.hamcrest.text.StringContainsInOrder


class TestProjectRequiresFields extends AbstractCopyIssueTest with JiraObjects with ShouldMatchersForJUnit {
	@Before def setUp {
		login(jira2)
	}

  @Rule def createIssues = new CreateIssues(restClient2)

	@Test def testMissingRequiredFieldsAreReported {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Some Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		var permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.areAllIssueFieldsRetained)
		Poller.waitUntilFalse(permissionChecksPage.areAllRequiredFieldsFilledIn)

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		assertThat(asJavaIterable(permissionChecksPage.getFieldGroups()
				.map(element => element.find(By.tagName("label")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String](
			"Due Date\nRequired", "Component/s\nRequired",
			"Affects Version/s\nRequired", "Fix Version/s\nRequired", "Environment\nRequired", "Description\nRequired",
			"Original Estimate\nRequired", "Remaining Estimate\nRequired", "Labels\nRequired"))

		permissionChecksPage = permissionChecksPage.submitWithErrors
		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		val errors = permissionChecksPage.getFieldGroups()
				.map(_.find(By.className("error")))
				.filter(_.isPresent)
				.map(element => element.getText).toIterable.asJava
		Assert.assertThat(errors, IsIterableContainingInOrder.contains[String](
			"Due Date is required.", "Component/s is required.", "Affects Version/s is required.",
			"Fix Version/s is required.",
			"Environment is required.", "Description is required.", "Original Estimate is required.",
			"Labels is required."))
	}

	@Test def shouldCopyIssueWithMissingRequiredFields {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Some Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
    val textFields = Map(
      "duedate"-> "16/Jan/13",
      "environment"-> "Mac OS X",
      "description" -> "This is a description.",
      "timetracking_originalestimate" -> "1w",
      "timetracking_remainingestimate" -> "1w"
    )
    textFields.foreach(k=> permissionChecksPage.getMappingResultFor(k._1).typeToTextField(k._2))

    val multiSelects = Map(
      "components" -> "Core",
      "versions" -> "1.0",
      "fixVersions" -> "1.0",
      "labels" -> "test"
    )
    multiSelects.foreach(k => permissionChecksPage.getMappingResultFor(k._1).setMultiSelect(k._2))


		val succesfulCopyPage = permissionChecksPage.copyIssue()
		assertTrue(succesfulCopyPage.isSuccessful)
	}

  @Test def shouldShowInformationWhenValueCannotBeUsed {
    val unmappedUser: String = "reallyStrangeUser"

    try{
      //create temporary user which is developer
      testkit2.usersAndGroups().addUser(unmappedUser, "rst", "Really Strange User", "really@strange.user")
      testkit2.usersAndGroups().addUserToGroup(unmappedUser, "jira-developers")
      //create issue and assing to user
      val issueBuilder = new IssueInputBuilder("DNEL", 3L, "Sample issue assigned to not mappable user").setAssigneeName(unmappedUser)
      val issue = createIssues.newIssue(issueBuilder.build())
      //remove user from developers - he can't be assigned anymore
      testkit2.usersAndGroups().removeUserFromGroup(unmappedUser, "jira-developers")

      //test locally - display values in a hover
      {
        val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], issue.getId)
        val permissionChecksPage = selectTargetProjectPage.next.next

        val assigneeRow: MappingResult = permissionChecksPage.getMappingResultFor("assignee")
        Poller.waitUntilTrue(assigneeRow.hasNotMappedNotify)
        Poller.waitUntil(assigneeRow.getUnmappedNotifyText, StringContainsInOrder.stringContainsInOrder(List("values cannot be copied", unmappedUser.toUpperCase)))
      }

      //test remotely - display unmapped values as plain text
      {
        val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], issue.getId)
        val permissionChecksPage = selectTargetProjectPage.setDestinationProject("Test").next.next

        val assigneeRow: MappingResult = permissionChecksPage.getMappingResultFor("assignee")
        Poller.waitUntil(assigneeRow.getMessage, StringContainsInOrder.stringContainsInOrder(List("values cannot be copied", unmappedUser.toUpperCase)))
        Poller.waitUntil(permissionChecksPage.getMappingResultFor("customfield_10201").getMessage, Matchers.containsString("field does not exist in the target project"))
      }

    } finally {
      testkit2.usersAndGroups().deleteUser(unmappedUser)
    }

  }

	@Test def shouldCopyIssueWithMissingRequiredCustomFields {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2
				.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Custom Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)

    val textFields = Map(
      "environment"-> "Mac OS X",
      "customfield_10004" -> "jira-administrators",
      "customfield_10005" -> "123"
    )
    textFields.foreach(k=> permissionChecksPage.getMappingResultFor(k._1).typeToTextField(k._2))
    permissionChecksPage.getMappingResultFor("versions").setMultiSelect("2.0")
		permissionChecksPage.getMappingResultFor("customfield_10006").getSelectElement.select(Options.text("beta"))

		val successfulPage = permissionChecksPage.copyIssue()
    assertTrue(successfulPage.isSuccessful)


    //copied issue should contain default values
    val copiedIssue = restClient2.getIssueClient.getIssue(successfulPage.getRemoteIssueKey).claim()

    copiedIssue.getFieldByName("NumberFieldWithDefault").getValue should equal(123456)

    val multiSelectVal = copiedIssue.getFieldByName("MultiSelectWithDefault").getValue.asInstanceOf[JSONArray]
    val pureValues = 0 until multiSelectVal.length() map(index => multiSelectVal.getJSONObject(index).getString("value"))
    pureValues should have length 3
    pureValues should be eq List("one", "three", "four").toIndexedSeq


	}
}
