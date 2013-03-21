package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstanceConfirmationPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.{Before, Test}
import org.junit.Assert._
import org.junit.Assert
import org.hamcrest.collection.IsIterableContainingInOrder
import org.openqa.selenium.By
import collection.JavaConverters._
import collection.JavaConversions._
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.pageobjects.elements.Options

class TestProjectRequiresFields extends AbstractCopyIssueTest with JiraObjects {
	@Before def setUp {
		login(jira2)
	}

	@Test def testMissingRequiredFieldsAreReported {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
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
			"Due Date is required.", "Component/s is required.", "Affects Version/s is required.", "Fix Version/s is required.",
			"Environment is required.", "Description is required.", "Original Estimate is required.", "Labels is required."))
	}

	@Test def shouldCopyIssueWithMissingRequiredFields {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Some Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		permissionChecksPage.typeToTextField("duedate", "16/Jan/13")
			.typeToTextField("environment", "Mac OS X")
			.typeToTextField("description", "This is a description.")
			.typeToTextField("timetracking_originalestimate", "1w")
			.typeToTextField("timetracking_remainingestimate", "1w")
			.setMultiSelect("components", "Core")
			.setMultiSelect("versions", "1.0")
			.setMultiSelect("fixVersions", "1.0")
			.setMultiSelect("labels", "test")

		val succesfulCopyPage = permissionChecksPage.copyIssue()
		assertTrue(succesfulCopyPage.isSuccessful)
	}

	@Test def shouldCopyIssueWithMissingRequiredCustomFields {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Custom Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		permissionChecksPage.typeToTextField("environment", "Mac OS X")
			.setMultiSelect("versions", "2.0")
			.typeToTextField("customfield_10004", "jira-administrators")
			.typeToTextField("customfield_10005", "123")

		permissionChecksPage.getSelectElement("customfield_10006").select(Options.text("beta"))

		val succesfulCopyPage = permissionChecksPage.copyIssue()
		assertTrue(succesfulCopyPage.isSuccessful)
	}
}
