package com.atlassian.cpji.fields.system

import java.lang.Long

import com.atlassian.cpji.rest.model.{CopyIssueBean, TimeTrackingBean}
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.{FieldManager, OrderableField, TimeTrackingSystemField}
import com.atlassian.jira.issue.{IssueFieldConstants, IssueInputParameters}
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.util.JiraDurationUtils
import org.junit.runner.RunWith
import org.junit.{Before, Test}
import org.mockito.Mockito._
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.{ArgumentMatchers, Mock}

@RunWith(classOf[MockitoJUnitRunner]) class TestTimeTrackingFieldMapper {

	@Mock var fieldManager: FieldManager = null
	@Mock var timeTrackingConfiguration: TimeTrackingConfiguration = null
	@Mock var issueInputParameters: IssueInputParameters = null
	@Mock var actionParams: java.util.Map[String, Array[String]] = null
	@Mock var jiraDurationUtils: JiraDurationUtils = null

	var timeTrackingFieldMapper: TimeTrackingFieldMapper = null
	var testCopyIssueBean: CopyIssueBean = null

	val worker = new MockComponentWorker()

	@Before def setUp {
		val of: OrderableField[Object] = mock(classOf[OrderableField[Object]])
		when(fieldManager.getField(IssueFieldConstants.TIMETRACKING)) thenReturn (of)

		timeTrackingFieldMapper = new TimeTrackingFieldMapper(fieldManager, timeTrackingConfiguration, null)

		testCopyIssueBean = new CopyIssueBean

		when(issueInputParameters.getActionParameters).thenReturn(actionParams)

		when(timeTrackingConfiguration.enabled()).thenReturn(true)

		ComponentAccessor.initialiseWorker(worker)
		worker.addMock(classOf[JiraDurationUtils], jiraDurationUtils)
	}

	@Test def shouldDoNothingWhenTimeTrackingIsOff() {
		when(timeTrackingConfiguration.enabled()).thenReturn(false)

		fire()

		verify(actionParams, never()).put(ArgumentMatchers.anyString(), ArgumentMatchers.any())
	}

	@Test def shouldDoNothingWhenTimeTrackingBeanIsEmpty() {

		testCopyIssueBean.setTimeTracking(null)
		fire()
		verify(actionParams, never()).put(ArgumentMatchers.anyString(), ArgumentMatchers.any())

		prepareTimeTrackingBean(null, null, null)
		fire()
		verify(actionParams, never()).put(ArgumentMatchers.anyString(), ArgumentMatchers.any())
	}


	@Test def shouldSetTimeTrackingToOrignalEstimateWhenLegacyModeIsEnabled() {
		prepareTimeTrackingBean()
		when(timeTrackingConfiguration.getMode).thenReturn(TimeTrackingConfiguration.Mode.LEGACY)
		when(jiraDurationUtils.getShortFormattedDuration(ArgumentMatchers.anyLong())).thenReturn("100")

		fire()

		verify(actionParams).put(IssueFieldConstants.TIMETRACKING, Array("100"))
		verify(actionParams, never).put(ArgumentMatchers.eq(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE), ArgumentMatchers.any())
		verify(actionParams, never).put(ArgumentMatchers.eq(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE), ArgumentMatchers.any())
	}

	@Test def shouldSetOriginalAndRemainingToOrignalEstimateWhenLegacyModeIsDisabled() {
		prepareTimeTrackingBean()
		when(timeTrackingConfiguration.getMode).thenReturn(TimeTrackingConfiguration.Mode.MODERN)
		when(jiraDurationUtils.getShortFormattedDuration(ArgumentMatchers.anyLong())).thenReturn("100")

		fire()

		verify(actionParams).put(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE, Array("100"))
		verify(actionParams).put(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE, Array("100"))
		verify(actionParams, never).put(ArgumentMatchers.eq(IssueFieldConstants.TIMETRACKING), ArgumentMatchers.any())
	}


	def fire() {
		timeTrackingFieldMapper.populateCurrentValue(issueInputParameters, testCopyIssueBean, null, null)
	}


	def prepareTimeTrackingBean(original: Long = 100L, estimate: Long = 60L, spent: Long = 40L): TimeTrackingBean = {
		val tt = new TimeTrackingBean(original, spent, estimate)
		testCopyIssueBean.setTimeTracking(tt)
		return tt
	}


}
