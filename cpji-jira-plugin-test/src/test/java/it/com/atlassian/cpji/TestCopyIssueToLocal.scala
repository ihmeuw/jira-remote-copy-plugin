package it.com.atlassian.cpji

import org.junit.{Test, Before}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{IssueInputBuilder, LinkIssuesInput, ComplexIssueInputFieldValue, FieldInput}
import java.lang.String
import com.atlassian.cpji.tests.pageobjects.{CopyDetailsPage, CopyIssueToInstanceConfirmationPage, SelectTargetProjectPage}
import org.junit.Assert._
import com.atlassian.jira.pageobjects.JiraTestedProduct
import com.atlassian.jira.webtests.Permissions
import org.hamcrest.Matchers
import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import com.atlassian.pageobjects.elements.query.Poller
import org.codehaus.jettison.json.JSONObject
import com.atlassian.jira.testkit.client.restclient.TimeTracking
import com.google.common.collect.ImmutableMap
import com.atlassian.jira.config.properties.APKeys
import org.hamcrest.collection.IsIterableContainingInAnyOrder

class TestCopyIssueToLocal extends AbstractCopyIssueTest with JiraObjects {

  implicit def fieldInputFromVals(arg: (IssueFieldId, AnyRef)) = new FieldInput(arg._1, arg._2)

  val createIssues3 = new CreateIssues(restClient3)

  @Before
  def setUp() {
    login(jira3)
  }

  val createIssueAdvanced : ((String, List[FieldInput] ) => Issue) = (summary, additionalFields) => {
    val fieldInputs : List[FieldInput] = List(
      (IssueFieldId.SUMMARY_FIELD, summary),
      (IssueFieldId.PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "WHEI")),
      (IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3"))
    )

    createIssues3.newIssue((fieldInputs ++ additionalFields):_*)
  }

  val createIssue = (summary : String)=>{
    createIssueAdvanced(summary, List())
  }


  val defaultPermissionChecksInteraction: (CopyIssueToInstanceConfirmationPage) => Unit = (page) => {
    Poller.waitUntilTrue(page.areAllIssueFieldsRetained)
    Poller.waitUntilTrue(page.areAllRequiredFieldsFilledIn)
  }

  private def remoteCopy(jira: JiraTestedProduct, issueId: Long,
                         selectTargetInteraction: (SelectTargetProjectPage) => Unit = (page) => {},
                         copyDetailsPageInteraction: (CopyDetailsPage) => Unit = (page) => {},
                         permissionsChecksInteraction: (CopyIssueToInstanceConfirmationPage) => Unit = defaultPermissionChecksInteraction): String = {
    val selectTargetProjectPage = jira.visit(classOf[SelectTargetProjectPage], issueId: java.lang.Long)
    selectTargetInteraction(selectTargetProjectPage)

    val copyDetailsPage = selectTargetProjectPage.next
    copyDetailsPageInteraction(copyDetailsPage)
    val permissionChecksPage: CopyIssueToInstanceConfirmationPage = copyDetailsPage.next
    permissionsChecksInteraction(permissionChecksPage)

    val copyIssueToInstancePage = permissionChecksPage.copyIssue
    assertTrue(copyIssueToInstancePage.isSuccessful)
    copyIssueToInstancePage.getRemoteIssueKey
  }

  @Test
  def copySimpleIssueWithTimeEstimationToDefaultProject() {
    testkit3.timeTracking().enable("8", "5", TimeTracking.Format.PRETTY, TimeTracking.Unit.MINUTE, TimeTracking.Mode.MODERN)
    val trackingParams : java.util.Map[String, AnyRef] = ImmutableMap.of("originalEstimate", "100", "remainingEstimate", "40")
    val issue = createIssueAdvanced("Sample issue", List(
      (IssueFieldId.TIMETRACKING_FIELD, new ComplexIssueInputFieldValue(trackingParams))
    ))

    val copiedIssueKey = remoteCopy(jira3, issue.getId,
      permissionsChecksInteraction = (page) => {
        Poller.waitUntilTrue(page.areAllRequiredFieldsFilledIn)
      })

    val copiedIssue = restClient3.getIssueClient.getIssue(copiedIssueKey, NPM)
    assertEquals(issue.getProject.getKey, copiedIssue.getProject.getKey)
    issuesEquals(issue, copiedIssue)
    assertEquals(100, copiedIssue.getTimeTracking.getOriginalEstimateMinutes)
    assertEquals(100, copiedIssue.getTimeTracking.getRemainingEstimateMinutes)
    assertNull(copiedIssue.getTimeTracking.getTimeSpentMinutes)
  }

  @Test
  def copyIssueWithAttachmentAndLink() {

    val issue = createIssue("Sample issue")

    {
      val issueToLink = createIssue("Issue to link")
      restClient3.getIssueClient.linkIssue(new LinkIssuesInput(issue.getKey, issueToLink.getKey, "Duplicate"), NPM)
    }

    restClient3.getIssueClient.addAttachment(NPM,
      issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)


    val copiedIssueKey = remoteCopy(jira3, issue.getId,
      permissionsChecksInteraction = (page) => {
        Poller.waitUntilTrue(page.areAllRequiredFieldsFilledIn)
      },
      copyDetailsPageInteraction = (page) => {
        //local clone cannot create unidirectional link
        assertThat(page.getCreateIssueLinks, IsIterableContainingInAnyOrder.containsInAnyOrder("Both directions", "None"))
      }
    )

    val copiedIssue = restClient3.getIssueClient.getIssue(copiedIssueKey, NPM)

    val issueWithExtras = restClient3.getIssueClient.getIssue(issue.getKey, NPM)

    issuesEquals(issueWithExtras, copiedIssue)
  }

  @Test
  def warningMessageIsDisplayedWhenUserHasNoRightsToCreateIssue() {

    val issue = createIssue("Sample issue")

    try {
      testkit3.permissionSchemes().removeProjectRolePermission(0L, Permissions.CREATE_ISSUE, 10000)
      viewIssue(jira3, issue.getKey)

      val selectTargetProjectPage = jira3.visit(classOf[SelectTargetProjectPage], issue.getId)

	  Poller.waitUntilTrue(selectTargetProjectPage.getTargetEntityWarningMessage.timed().isPresent)
      assertThat(selectTargetProjectPage.getTargetEntityWarningMessage.getText, Matchers.startsWith("You cannot clone issue"))
    } finally {
      testkit3.permissionSchemes().addProjectRolePermission(0L, Permissions.CREATE_ISSUE, 10000)
    }

  }


	@Test
	def cloningSubtaskAsAnotherSubtask() {
		try {
			testkit3.subtask().enable()

			val parentIssue = createIssue("Sample parent")
			val subtaskBuilder = new IssueInputBuilder("WHEI", 5L, "Sample child").setFieldValue("parent", ComplexIssueInputFieldValue.`with`("key", parentIssue.getKey))
			val subtask = createIssues3.newIssue(subtaskBuilder.build())

			val copiedIssueKey = remoteCopy(jira3, subtask.getId, permissionsChecksInteraction = (page) => {
        Poller.waitUntilTrue(page.areAllRequiredFieldsFilledIn)
			})

			val copiedIssue = restClient3.getIssueClient.getIssue(copiedIssueKey, NPM)

			issuesEquals(subtask, copiedIssue)

			val subTaskParent : JSONObject = subtask.getField("parent").getValue.asInstanceOf[JSONObject]
			val copiedIssueParent : JSONObject = copiedIssue.getField("parent").getValue.asInstanceOf[JSONObject]

			assertEquals(subTaskParent.get("key"), copiedIssueParent.get("key"))
		} finally {
			testkit3.subtask().disable()
		}

	}


	@Test
	def cloningSubtaskToAnotherProject() {
		try {
			testkit3.subtask().enable()

			val parentIssue = createIssue("Sample parent")
			val subtaskBuilder = new IssueInputBuilder("WHEI", 5L, "Sample child").setFieldValue("parent", ComplexIssueInputFieldValue.`with`("key", parentIssue.getKey))
			val subtask = createIssues3.newIssue(subtaskBuilder.build())

			val copiedIssueKey = remoteCopy(jira3, subtask.getId, permissionsChecksInteraction = (page) => {
        Poller.waitUntilTrue(page.areAllRequiredFieldsFilledIn)
			},
				selectTargetInteraction = (page) => page.setDestinationProject("acrobata")
			)

			val copiedIssue = restClient3.getIssueClient.getIssue(copiedIssueKey, NPM)

			assertEquals("CLONE - " + subtask.getSummary, copiedIssue.getSummary)
			assertEquals("AOBA", copiedIssue.getProject.getKey)

			val copiedIssueParent = copiedIssue.getField("parent")
			assertNull(copiedIssueParent)

		} finally {
			testkit3.subtask().disable()
		}

	}


  def issuesEquals(issue: Issue, copiedIssue: Issue) {
    val eq = (field: (Issue => AnyRef)) => {
      assertEquals(field(issue), field(copiedIssue))
    }

		assertEquals("CLONE - " + issue.getSummary, copiedIssue.getSummary)
    eq(_.getAssignee)
		eq(_.getIssueType)
    eq(_.getAttachments.asScala.map(x => (x.getSize, x.getFilename, x.getMimeType)))
		//issue links should equals (of course without links between theese two issues)
    eq(_.getIssueLinks.asScala
			.filter(x => (x.getTargetIssueId != issue.getId && x.getTargetIssueId != copiedIssue.getId))
			.map(x => (x.getIssueLinkType, x.getTargetIssueId, x.getTargetIssueKey))
		)
  }

}
