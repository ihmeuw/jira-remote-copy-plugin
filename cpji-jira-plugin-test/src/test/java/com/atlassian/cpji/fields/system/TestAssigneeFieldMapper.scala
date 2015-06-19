package com.atlassian.cpji.fields.system

import com.atlassian.cpji.fields.system.AssigneeFieldMapper.InternalMappingResult
import com.atlassian.cpji.fields.value.{CachingUserMapper, DefaultFieldValuesManager, UserMappingManager}
import com.atlassian.cpji.rest.model.{CopyIssueBean, UserBean}
import com.atlassian.jira.config.properties.{APKeys, ApplicationProperties}
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.fields.{FieldManager, OrderableField}
import com.atlassian.jira.issue.{IssueFieldConstants, IssueInputParameters}
import com.atlassian.jira.project.{AssigneeTypes, Project}
import com.atlassian.jira.security.{PermissionManager, Permissions}
import com.atlassian.jira.user.ApplicationUser
import org.junit.runner.RunWith
import org.junit.{Before, Test}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.{Matchers, Mock}
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[MockitoJUnitRunner])
class TestAssigneeFieldMapper extends ShouldMatchersForJUnit with MockitoSugar {


  var asigneeFieldMapper: AssigneeFieldMapper = null

  //components
  @Mock var userMappingManager: UserMappingManager = null
  @Mock var userMapper: CachingUserMapper = null
  @Mock var permissionManager: PermissionManager = null
  @Mock var applicationProperties: ApplicationProperties = null
  @Mock var defaultValuesManager: DefaultFieldValuesManager = null

  //data objects
  @Mock var project: Project = null
  @Mock var userBean: UserBean = null
  @Mock var projectLead: ApplicationUser = null
  @Mock var mappedUser: ApplicationUser = null
  @Mock var inputParams: IssueInputParameters = null
  @Mock var asigneeField: OrderableField[_] = null

  var plugedMapMethod = false
  var pluggedMappingResult: InternalMappingResult = null

  @Before def setUp {
    plugedMapMethod = false

    val fieldManager = mock[FieldManager]
    when(asigneeField.getId).thenReturn(IssueFieldConstants.ASSIGNEE)
    when(fieldManager.getField(IssueFieldConstants.ASSIGNEE)).thenReturn(asigneeField)

    when(project.getLead).thenReturn(projectLead)

    asigneeFieldMapper = new AssigneeFieldMapper(permissionManager, applicationProperties, fieldManager, defaultValuesManager) {
      override def mapUser(userMapper: CachingUserMapper, user: UserBean, project: Project): InternalMappingResult = {
        if (plugedMapMethod) {
          pluggedMappingResult
        } else
          super.mapUser(userMapper, user, project)
      }
    }
  }

  //mappings

  @Test def mappingShouldReturnNotFoundWhenNoUserBeanGiven() {
    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND, null)
  }

  @Test def mappingShouldReturnNotFoundWhenUserCannotBeMappedAndDefaulAssigneeIsNotSet() {
    leaveIssuesUnassigned
    userIsNotMapped

    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND)
  }

  @Test def mappingShouldReturnMappedAssigneeWhenIsMappableAndHasPermission() {
    userIsWellMapped
    userCanBeAssigned

    mapAndTest(mappedUser, InternalMappingResult.MappingResultDecision.FOUND)
  }

  @Test def mappingShouldReturnNotFoundWhenUserIsMappedAndHasNoPermission() {
    leaveIssuesUnassigned
    userIsWellMapped
    userCannotBeAssigned
    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND)
  }


  def mapAndTest(desiredUser: ApplicationUser, desiredDecision: InternalMappingResult.MappingResultDecision, userBean: UserBean = this.userBean) {
    val result = asigneeFieldMapper.mapUser(userMapper, userBean, project)
    result.mappedUser should equal(desiredUser)
    result.decision should equal(result.decision)
  }

  def leaveIssuesUnassigned = when(project.getAssigneeType).thenReturn(AssigneeTypes.UNASSIGNED)

  def userIsNotMapped = when(userMapper.mapUser(userBean)).thenReturn(null)

  def userIsWellMapped = when(userMapper.mapUser(userBean)).thenReturn(mappedUser)

  def userCanBeAssigned = when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(true)

  def userCannotBeAssigned = when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(false)

  //population

  @Test def shouldPopulateParametersWhenUserIsFound() {

    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    when(mappedUser.getName).thenReturn("fred")
    plugMapMethod(new InternalMappingResult(mappedUser, InternalMappingResult.MappingResultDecision.FOUND))
    asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, null, null, null)
    verify(inputParams).setAssigneeId("fred")
  }

  @Test def shouldDoNothingWhenMappingIsNotFoundAndUnassignedAreAllowed() {
    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    plugMapMethod(new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND))
    when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(true)

    asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, null, project, null)
    verify(inputParams, never()).setAssigneeId(Matchers.any())
  }

  @Test def shouldPopulateWithDefaultValueWhenItIsSet() {
    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    plugMapMethod(new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND))
    when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(false)

    val fieldLayoutItem = mock[FieldLayoutItem]
    when(fieldLayoutItem.getOrderableField).thenReturn(asigneeField)
    when(defaultValuesManager.hasDefaultValue(any(), any(), any())).thenReturn(true)
    when(defaultValuesManager.getDefaultFieldValue(any(), any(), any())).thenReturn(Array("defaultAsgn"))

    asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, fieldLayoutItem, project, null)
    verify(inputParams).setAssigneeId("defaultAsgn")
  }


  @Test def shouldPopulateWithAutomaticAssigneeWhenNoDefaultValueIsDefined() {
    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    plugMapMethod(new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND))
    when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(false)

    val fieldLayoutItem = mock[FieldLayoutItem]
    when(fieldLayoutItem.getOrderableField).thenReturn(asigneeField)
    when(defaultValuesManager.hasDefaultValue(any(), any(), any())).thenReturn(false)

    asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, fieldLayoutItem, project, null)
    verify(inputParams).setAssigneeId("-1") //jira should choose assignee automatically
  }


  def plugMapMethod(result: InternalMappingResult) {
    pluggedMappingResult = result
    plugedMapMethod = true
  }


}
