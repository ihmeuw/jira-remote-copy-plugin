package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage
import com.atlassian.jira.rest.client.api.domain.IssueField
import com.google.common.collect.Iterables
import org.hamcrest.collection.IsIterableContainingInAnyOrder
import org.junit.Assert.{assertEquals, assertThat, assertTrue}
import org.junit.{Before, Test}

import scala.collection.JavaConversions._

class TestCopyCustomFields extends AbstractCopyIssueTest with JiraObjects {
	@Before def setup() {
		login(jira2)
	}

	@Test
	def testCopyLocally() {
		val issueWeWantToCopy = testkit2.issues().getIssue("ACF-1")
		val issueId: java.lang.Long = java.lang.Long.parseLong(issueWeWantToCopy.id)
		val selectTargetPage = jira2.visit(classOf[SelectTargetProjectPage], issueId)
		val copiedIssuePage = selectTargetPage.next().next().copyIssue()
		assertTrue(copiedIssuePage.isSuccessful)

		val sourceIssue = restClient2.getIssueClient.getIssue("ACF-1").claim()
		val copiedIssue = restClient2.getIssueClient.getIssue(copiedIssuePage.getRemoteIssueKey).claim()

		val fields1 = List(Iterables.toArray(sourceIssue.getFields, classOf[IssueField]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		val fields2 = List(Iterables.toArray(copiedIssue.getFields, classOf[IssueField]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		assertEquals(20, fields1.length)
		assertEquals(fields2, fields1)

		// should have at least those (may have more in the future)
		assertThat(asJavaIterable(fields2.map(_._1)), IsIterableContainingInAnyOrder.containsInAnyOrder("Cascading Select",
			"Custom labels", "Date Time", "DatePickerCF", "FreeTextFieldCF", "Group Picker", "GroupPickerCF",
			"Multi Select", "Multi User Picker", "Multi checkboxes", "MultiGroupPickerCF", "NumberFieldCF", "Project Picker",
			"Radio Buttons", "SelectListCF", "Single Version Picker", "Text Field", "URL Field", "User Picker", "Version Picker"))
	}
}
