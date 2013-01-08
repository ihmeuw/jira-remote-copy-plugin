package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{PermissionChecksPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.{Before, Test}
import org.junit.Assert._
import com.atlassian.jira.pageobjects.JiraTestedProduct
import org.hamcrest.collection.IsIterableContainingInOrder
import com.atlassian.pageobjects.elements.PageElement
import org.openqa.selenium.By
import collection.JavaConversions._
import com.atlassian.pageobjects.elements.query.Poller

class TestProjectRequiresFields extends AbstractCopyIssueTest {
	var jira2 : JiraTestedProduct = null

	@Before def setUp {
		jira2 = AbstractCopyIssueTest.jira2
		login(jira2)
	}

	@Test def testMissingRequiredFieldsAreReported {
		val selectTargetProjectPage: SelectTargetProjectPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10105L))
		selectTargetProjectPage.setDestinationProject("Some Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		var permissionChecksPage: PermissionChecksPage = copyDetailsPage.next
		assertFalse(permissionChecksPage.isAllSystemFieldsRetained)
		assertTrue(permissionChecksPage.isAllCustomFieldsRetained)

		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		assertThat(asJavaIterable(permissionChecksPage.getFieldGroups()
				.map(element => element.find(By.tagName("label")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String](
			"Due Date\nRequired", "Component/s\nRequired",
			"Affects Version/s\nRequired", "Fix Version/s\nRequired", "Environment\nRequired", "Description\nRequired",
			"Original Estimate\nRequired", "Remaining Estimate\nRequired", "Labels\nRequired", "Watchers"))

		permissionChecksPage = permissionChecksPage.submitWithErrors
		Poller.waitUntilTrue(permissionChecksPage.getFirstFieldGroup.isVisible)
		assertThat(asJavaIterable(permissionChecksPage.getFieldGroups()
				.filter(element => !element.findAll(By.className("error")).isEmpty)
				.map(element => element.find(By.className("error")))
				.map(element => element.getText).toIterable), IsIterableContainingInOrder.contains[String](
			"Due Date is required.", "Component/s is required.", "Affects Version/s is required.", "Fix Version/s is required.",
			"Environment is required.", "Description is required.", "Original Estimate is required.", "Labels is required."))
	}

}
