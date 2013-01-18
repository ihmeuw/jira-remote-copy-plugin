package it.com.atlassian.cpji

import org.junit.{Test, Before}
import org.junit.Assert.{assertTrue, assertEquals}
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage
import com.google.common.collect.Iterables
import com.atlassian.jira.rest.client.domain.Field

class TestCopyCustomFields extends AbstractCopyIssueTest with JiraObjects {
	@Before def setup {
		login(jira2)
	}

	@Test
	def testCopyLocally {
		val selectTargetPage = jira2.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10200))
		val copiedIssuePage = selectTargetPage.next().next().copyIssue()
		assertTrue(copiedIssuePage.isSuccessful)

		val i1 = restClient2.getIssueClient.getIssue("BLA-9", NPM)
		val i2 = restClient2.getIssueClient.getIssue(copiedIssuePage.getRemoteIssueKey, NPM)

		val fields1 = List(Iterables.toArray(i1.getFields, classOf[Field]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		val fields2 = List(Iterables.toArray(i2.getFields, classOf[Field]):_*)
				.filter(p => p.getId.startsWith("customfield_") && p.getValue != null).map(f => (f.getName, f.getValue.toString)).sortBy(_._1)
		assertEquals(21, fields1.length)
		assertEquals(fields2, fields1)
	}
}
