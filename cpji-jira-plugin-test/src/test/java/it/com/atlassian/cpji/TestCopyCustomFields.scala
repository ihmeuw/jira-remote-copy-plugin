package it.com.atlassian.cpji

import org.junit.{Test, Before}
import org.junit.Assert.{assertTrue, assertEquals, assertThat}
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage
import com.google.common.collect.Iterables
import com.atlassian.jira.rest.client.domain.Field
import collection.JavaConversions._
import org.hamcrest.collection.IsIterableContainingInAnyOrder

class TestCopyCustomFields extends AbstractCopyIssueTest with JiraObjects {
	@Before def setup {
		login(jira2)
	}

	@Test
	def testCopyLocally {
		val selectTargetPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10200))
		val copiedIssuePage = selectTargetPage.next().next().copyIssue()
		assertTrue(copiedIssuePage.isSuccessful)

		val i1 = restClient2.getIssueClient.getIssue("ACF-1", NPM)
		val i2 = restClient2.getIssueClient.getIssue(copiedIssuePage.getRemoteIssueKey, NPM)

		val fields1 = List(Iterables.toArray(i1.getFields, classOf[Field]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		val fields2 = List(Iterables.toArray(i2.getFields, classOf[Field]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		assertEquals(22, fields1.length)
		assertEquals(fields2, fields1)

		// should have at least those (may have more in the future)
		assertThat(asJavaIterable(fields2.map(_._1)), IsIterableContainingInAnyOrder.containsInAnyOrder("Cascading Select",
			"Custom labels", "Date Time", "DatePickerCF", "FreeTextFieldCF", "Group Picker", "GroupPickerCF", "Hidden Job Switch", "Job checkbox",
			"Multi Select", "Multi User Picker", "Multi checkboxes", "MultiGroupPickerCF", "NumberFieldCF", "Project Picker",
			"Radio Buttons", "SelectListCF", "Single Version Picker", "Text Field", "URL Field", "User Picker", "Version Picker"))
	}
}
