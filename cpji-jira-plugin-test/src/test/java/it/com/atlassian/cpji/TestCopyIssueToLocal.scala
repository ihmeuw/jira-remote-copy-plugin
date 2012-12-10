package it.com.atlassian.cpji

import org.junit.{Test, Before}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import java.lang.{String, Object}
import com.atlassian.cpji.tests.pageobjects.{CopyIssueToInstancePage, PermissionChecksPage, CopyDetailsPage, SelectTargetProjectPage}
import org.junit.Assert._
import com.atlassian.jira.pageobjects.JiraTestedProduct

class TestCopyIssueToLocal extends AbstractCopyIssueTest{

  implicit def fieldInputFromVals(arg: (IssueFieldId, AnyRef)) = new FieldInput(arg._1, arg._2)
  val createIssues3 = new CreateIssues(AbstractCopyIssueTest.restClient3)
  @Before
  def setUp(){
    login(AbstractCopyIssueTest.jira3);
  }

  val createIssue = (summary: String) => {
    createIssues3.newIssue(
      (IssueFieldId.SUMMARY_FIELD, summary),
      (IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "WHEI")),
      (IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))
    )
  }


  private def remoteCopy(jira: JiraTestedProduct, issueKey: String, issueId: Long, destinationProject: String,
      selectTargetInteraction : (SelectTargetProjectPage)=>Unit = (page) =>{} ,
      permissionsChecksInteraction : (PermissionChecksPage)=>Unit = (page)=>{
        assertTrue(page.isAllSystemFieldsRetained)
        assertTrue(page.isAllCustomFieldsRetained)
      }

    ): String = {
    viewIssue(jira, issueKey)

    val selectTargetProjectPage = jira.visit(classOf[SelectTargetProjectPage], issueId : java.lang.Long)
    selectTargetInteraction(selectTargetProjectPage)

    val copyDetailsPage = selectTargetProjectPage.next
    val permissionChecksPage: PermissionChecksPage = copyDetailsPage.next
    permissionsChecksInteraction(permissionChecksPage)

    val copyIssueToInstancePage = permissionChecksPage.copyIssue
    assertTrue(copyIssueToInstancePage.isSuccessful)
    copyIssueToInstancePage.getRemoteIssueKey
  }

  @Test
  def copySimpleIssueToDefaultProject(){

    val issue = createIssue("Sample issue")
    val copiedIssueKey = remoteCopy(AbstractCopyIssueTest.jira3, issue.getKey, issue.getId, "witherbyi",permissionsChecksInteraction = (page)=>{
      assertTrue(page.isAllCustomFieldsRetained)
    });

    val copiedIssue = AbstractCopyIssueTest.restClient3.getIssueClient.getIssue(copiedIssueKey, AbstractCopyIssueTest.NPM)
    issuesEquals(issue, copiedIssue)
  }


  def issuesEquals(a:Issue, b:Issue){

    val eq = (field: (Issue=>AnyRef))=>{
      assertEquals(field(a), field(b))
    }
    eq(_.getSummary)
    eq(_.getAssignee)
  }

}
