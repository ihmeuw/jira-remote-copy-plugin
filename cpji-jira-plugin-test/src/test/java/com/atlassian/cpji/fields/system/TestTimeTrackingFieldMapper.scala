package com.atlassian.cpji.fields.system

import com.atlassian.jira.issue.fields.{TimeTrackingSystemField, OrderableField, FieldManager}
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration
import com.atlassian.jira.issue.{IssueFieldConstants, IssueInputParameters}
import com.atlassian.cpji.rest.model.{TimeTrackingBean, CopyIssueBean}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mock}
import org.junit.{Test, Before}
import org.mockito.Matchers._
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.lang.Long
import com.atlassian.core.util.DateUtils

@RunWith(classOf[MockitoJUnitRunner]) class TestTimeTrackingFieldMapper {

  @Mock var fieldManager: FieldManager = null
  @Mock var timeTrackingConfiguration: TimeTrackingConfiguration  = null
  @Mock var issueInputParameters: IssueInputParameters = null
  @Mock var actionParams : java.util.Map[String, Array[String]] = null


  var timeTrackingFieldMapper: TimeTrackingFieldMapper = null
  var testCopyIssueBean: CopyIssueBean = null

  @Before def setUp {
    val of: OrderableField = mock(classOf[OrderableField])
    when(fieldManager.getField(IssueFieldConstants.TIMETRACKING)) thenReturn(of)

    timeTrackingFieldMapper = new TimeTrackingFieldMapper(fieldManager, timeTrackingConfiguration, null)

    testCopyIssueBean = new CopyIssueBean

    when(issueInputParameters.getActionParameters).thenReturn(actionParams)

    when(timeTrackingConfiguration.enabled()).thenReturn(true)
    when(timeTrackingConfiguration.getDefaultUnit).thenReturn(DateUtils.Duration.SECOND)
  }

  @Test def shouldDoNothingWhenTimeTrackingIsOff() {
    when(timeTrackingConfiguration.enabled()).thenReturn(false)

    fire()

    verify(actionParams, never()).put(anyString,  any())
  }

  @Test def shouldDoNothingWhenTimeTrackingBeanIsEmpty() {

    testCopyIssueBean.setTimeTracking(null)
    fire()
    verify(actionParams, never()).put(anyString,  any())

    prepareTimeTrackingBean(null, null, null)
    fire()
    verify(actionParams, never()).put(anyString,  any())
  }


  @Test def shouldSetTimeTrackingToOrignalEstimateWhenLegacyModeIsEnabled() {
    prepareTimeTrackingBean()
    when(timeTrackingConfiguration.getMode).thenReturn(TimeTrackingConfiguration.Mode.LEGACY)

    fire()

    verify(actionParams).put(IssueFieldConstants.TIMETRACKING, Array("100"))
    verify(actionParams, never).put(Matchers.eq(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE), any())
    verify(actionParams, never).put(Matchers.eq(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE), any())
  }

  @Test def shouldSetOriginalAndRemainingToOrignalEstimateWhenLegacyModeIsDisabled() {
    prepareTimeTrackingBean()
    when(timeTrackingConfiguration.getMode).thenReturn(TimeTrackingConfiguration.Mode.MODERN)

    fire()

    verify(actionParams).put(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE,  Array("100"))
    verify(actionParams).put(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE,  Array("100"))
    verify(actionParams, never).put(Matchers.eq(IssueFieldConstants.TIMETRACKING), any())
  }


  def fire() {
    timeTrackingFieldMapper.populateInputParameters(issueInputParameters, testCopyIssueBean, null, null)
  }


  def prepareTimeTrackingBean(original: Long = 100L, estimate : Long = 60L, spent : Long = 40L) : TimeTrackingBean = {
    val tt = new TimeTrackingBean(original, spent, estimate)
    testCopyIssueBean.setTimeTracking(tt)
    return tt
  }




}
