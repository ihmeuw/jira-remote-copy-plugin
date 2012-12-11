package it.com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstancePage, PermissionChecksPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.Assert._
import java.lang.String
import org.hamcrest.Matchers._
import org.json.JSONObject
import com.atlassian.cpji.tests.RawRestUtil._
import org.junit.{Before, Test}
import com.atlassian.jira.pageobjects.JiraTestedProduct

class TestProjectRequiresFields extends AbstractCopyIssueTest {
	var jira1 : JiraTestedProduct = null
	var jira2 : JiraTestedProduct = null

	@Before def setUp {
		jira1 = AbstractCopyIssueTest.jira1
		jira2 = AbstractCopyIssueTest.jira2
	}

	@Test def testCopyForProjectWithoutProjectEntityLinks {
		viewIssue(jira1, "NEL-3")
		val selectTargetProjectPage: SelectTargetProjectPage = jira1.visit(classOf[SelectTargetProjectPage], new java.lang.Long(10301L))
		selectTargetProjectPage.setDestinationProject("Some Fields Required")
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next
		val permissionChecksPage: PermissionChecksPage = copyDetailsPage.next
		assertTrue(permissionChecksPage.isAllSystemFieldsRetained)
		assertTrue(permissionChecksPage.isAllCustomFieldsRetained)
		val copyIssueToInstancePage: CopyIssueToInstancePage = permissionChecksPage.copyIssue
		assertTrue(copyIssueToInstancePage.isSuccessful)
		val remoteIssueKey: String = copyIssueToInstancePage.getRemoteIssueKey
		assertThat(remoteIssueKey, startsWith("DNEL"))
		val json: JSONObject = getIssueJson(jira2, remoteIssueKey)
		val fields: JSONObject = json.getJSONObject("fields")
		assertEquals("Testing as admin", fields.getString("summary"))
		assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"))
		assertEquals(JSONObject.NULL, fields.opt("description"))
	}
}
