package it.com.atlassian.cpji

import org.junit.{Test, Before}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import java.lang.String
import com.atlassian.cpji.tests.pageobjects.{PermissionChecksPage, SelectTargetProjectPage}
import org.junit.Assert._
import com.atlassian.jira.pageobjects.JiraTestedProduct
import com.atlassian.jira.webtests.Permissions
import org.hamcrest.Matchers
import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import com.atlassian.pageobjects.elements.query.Poller

class TestCopyIssueToLocal extends AbstractCopyIssueTest {

  implicit def fieldInputFromVals(arg: (IssueFieldId, AnyRef)) = new FieldInput(arg._1, arg._2)

  val createIssues3 = new CreateIssues(AbstractCopyIssueTest.restClient3)

  @Before
  def setUp() {
    login(AbstractCopyIssueTest.jira3);
  }

  val createIssue = (summary: String) => {
    createIssues3.newIssue(
      (IssueFieldId.SUMMARY_FIELD, summary),
      (IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "WHEI")),
      (IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))
    )
  }

  val defaultPermissionChecksInteraction: (PermissionChecksPage) => Unit = (page) => {
    assertTrue(page.isAllSystemFieldsRetained)
    assertTrue(page.isAllCustomFieldsRetained)
  }

  private def remoteCopy(jira: JiraTestedProduct, issueKey: String, issueId: Long, destinationProject: String,
                         selectTargetInteraction: (SelectTargetProjectPage) => Unit = (page) => {},
                         permissionsChecksInteraction: (PermissionChecksPage) => Unit = defaultPermissionChecksInteraction): String = {
    viewIssue(jira, issueKey)

    val selectTargetProjectPage = jira.visit(classOf[SelectTargetProjectPage], issueId: java.lang.Long)
    selectTargetInteraction(selectTargetProjectPage)

    val copyDetailsPage = selectTargetProjectPage.next
    val permissionChecksPage: PermissionChecksPage = copyDetailsPage.next
    permissionsChecksInteraction(permissionChecksPage)

    val copyIssueToInstancePage = permissionChecksPage.copyIssue
    assertTrue(copyIssueToInstancePage.isSuccessful)
    copyIssueToInstancePage.getRemoteIssueKey
  }

  @Test
  def copySimpleIssueToDefaultProject() {

    val issue = createIssue("Sample issue")
    val copiedIssueKey = remoteCopy(AbstractCopyIssueTest.jira3, issue.getKey, issue.getId, "witherbyi",
      permissionsChecksInteraction = (page) => {
        assertTrue(page.isAllCustomFieldsRetained)
      })

    val copiedIssue = AbstractCopyIssueTest.restClient3.getIssueClient
      .getIssue(copiedIssueKey, AbstractCopyIssueTest.NPM)
    assertEquals(issue.getProject.getKey, copiedIssue.getProject.getKey)
    issuesEquals(issue, copiedIssue)
  }

  @Test
  def copyIssueWithAttachmentAndLink() {

    val issue = createIssue("Sample issue")

    {
      val issueToLink = createIssue("Issue to link")
      AbstractCopyIssueTest.restClient3.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, issueToLink.getKey, "Duplicate"), AbstractCopyIssueTest.NPM)
    }

    AbstractCopyIssueTest.restClient3.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
      issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)


    val copiedIssueKey = remoteCopy(AbstractCopyIssueTest.jira3, issue.getKey, issue.getId, "witherbyi",
      permissionsChecksInteraction = (page) => {
        assertTrue(page.isAllCustomFieldsRetained)
      })

    val copiedIssue = AbstractCopyIssueTest.restClient3.getIssueClient
      .getIssue(copiedIssueKey, AbstractCopyIssueTest.NPM)

    val issueWithExtras = AbstractCopyIssueTest.restClient3.getIssueClient
      .getIssue(issue.getKey, AbstractCopyIssueTest.NPM)

    issuesEquals(issueWithExtras, copiedIssue)
  }

  @Test
  def warningMessageIsDisplayedWhenUserHasNoRightsToCreateIssue() {

    val issue = createIssue("Sample issue")

    try {
      AbstractCopyIssueTest.testkit3.permissionSchemes().removeProjectRolePermission(0L, Permissions.CREATE_ISSUE, 10000)
      viewIssue(AbstractCopyIssueTest.jira3, issue.getKey)

      val selectTargetProjectPage = AbstractCopyIssueTest.jira3.visit(classOf[SelectTargetProjectPage], issue.getId)

	  Poller.waitUntilTrue(selectTargetProjectPage.getTargetEntityWarningMessage.timed().isPresent)
      assertThat(selectTargetProjectPage.getTargetEntityWarningMessage.getText, Matchers.startsWith("You can't clone issue"))
    } finally {
      AbstractCopyIssueTest.testkit3.permissionSchemes().addProjectRolePermission(0L, Permissions.CREATE_ISSUE, 10000)
    }

  }


  def issuesEquals(a: Issue, b: Issue) {
    val eq = (field: (Issue => AnyRef)) => {
      assertEquals(field(a), field(b))
    }

    eq(_.getSummary)
    eq(_.getAssignee)
    eq(_.getAttachments.asScala.map(x => (x.getSize, x.getFilename, x.getMimeType)))
    eq(_.getIssueLinks.asScala.map(x => (x.getIssueLinkType, x.getTargetIssueId, x.getTargetIssueKey)))
  }

}
