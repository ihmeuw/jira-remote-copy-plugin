package com.atlassian.cpji.fields.system

import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.junit.{Test, Before}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mock}
import com.atlassian.jira.project.{AssigneeTypes, Project}
import org.junit.Assert.{assertNull, assertEquals}
import com.atlassian.cpji.fields.system.AssigneeFieldMapper.InternalMappingResult
import com.atlassian.jira.issue.fields.{OrderableField, FieldManager}
import com.atlassian.jira.issue.{IssueInputParameters, IssueFieldConstants}
import com.atlassian.cpji.fields.value.UserMappingManager
import com.atlassian.cpji.rest.model.{CopyIssueBean, UserBean}
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.security.{PermissionManager, Permissions}
import com.atlassian.cpji.fields.MappingResult
import com.atlassian.jira.config.properties.{APKeys, ApplicationProperties}

@RunWith(classOf[MockitoJUnitRunner]) class TestAsigneeFieldMapper {


  var asigneeFieldMapper : AssigneeFieldMapper = null

  //components
  @Mock var userMappingManager : UserMappingManager = null
  @Mock var permissionManager : PermissionManager = null
  @Mock var applicationProperties : ApplicationProperties = null

  //data objects
  @Mock var project : Project = null
  @Mock var userBean :UserBean = null
  @Mock var projectLead : User = null
  @Mock var mappedUser : User = null
  @Mock var inputParams : IssueInputParameters = null

  var plugedMapMethod = false
  var pluggedMappingResult : InternalMappingResult = null

  @Before def setUp {
    plugedMapMethod = false

    val fieldManager = mock(classOf[FieldManager])
    val assigneeField = mock(classOf[OrderableField])
    when(fieldManager.getField(IssueFieldConstants.ASSIGNEE)).thenReturn(assigneeField)

    when(project.getLead).thenReturn(projectLead)

    asigneeFieldMapper = new AssigneeFieldMapper(permissionManager, applicationProperties, fieldManager, userMappingManager, null){
      override def mapUser(user: UserBean, project: Project): InternalMappingResult = {
        if (plugedMapMethod){
          pluggedMappingResult
        } else
          super.mapUser(user, project)
      }
    }
  }

  @Test def mappingShouldReturnNotFoundWhenNoUserBeanGiven(){
    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND, null)
  }

  @Test def mappingShouldReturnDefaultAssigneeWhenUserCannotBeMappedAndProjectLeadIsDefaultAssignee(){
    projectLeadIsDefaultAssignee
    userIsNotMapped

    mapAndTest(projectLead, InternalMappingResult.MappingResultDecision.DEFAULT_ASSIGNEE_USED)
  }

  @Test def mappingShouldReturnNotFoundWhenUserCannotBeMappedAndDefaulAssigneeIsNotSet(){
    leaveIssuesUnassigned
    userIsNotMapped

    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND)
  }

  @Test def mappingShouldReturnMappedAssigneeWhenIsMappableAndHasPermission(){
    userIsWellMapped
    userCanBeAssigned

    mapAndTest(mappedUser, InternalMappingResult.MappingResultDecision.FOUND)
  }


  @Test def mappingShouldReturnDefaultAssigneeWhenUserIsMappedAndHasNoPermission(){
    projectLeadIsDefaultAssignee

    userIsWellMapped
    userCannotBeAssigned

    mapAndTest(projectLead, InternalMappingResult.MappingResultDecision.DEFAULT_ASSIGNEE_USED)
  }

  @Test def mappingShouldReturnNotFoundWhenUserIsMappedAndHasNoPermission(){
    leaveIssuesUnassigned
    userIsWellMapped
    userCannotBeAssigned
    mapAndTest(null, InternalMappingResult.MappingResultDecision.NOT_FOUND )
  }

  def leaveIssuesUnassigned = when(project.getAssigneeType).thenReturn(AssigneeTypes.UNASSIGNED)
  def projectLeadIsDefaultAssignee = when(project.getAssigneeType).thenReturn(AssigneeTypes.PROJECT_LEAD)
  def userIsNotMapped =  when(userMappingManager.mapUser(userBean, project)).thenReturn(null)
  def userIsWellMapped = when(userMappingManager.mapUser(userBean, project)).thenReturn(mappedUser)
  def userCanBeAssigned = when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(true)
  def userCannotBeAssigned = when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(false)

  @Test def shouldPopulateParametersWhenUserIsFoundOrDefaultAssignee(){

    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    when(mappedUser.getName).thenReturn("fred")
    plugMapMethod(new InternalMappingResult(mappedUser, InternalMappingResult.MappingResultDecision.FOUND))
    asigneeFieldMapper.populateInputParameters(inputParams, bean, null, null)
    verify(inputParams).setAssigneeId("fred")

    when(mappedUser.getName).thenReturn("fred2")
    plugMapMethod(new InternalMappingResult(mappedUser, InternalMappingResult.MappingResultDecision.DEFAULT_ASSIGNEE_USED))
    asigneeFieldMapper.populateInputParameters(inputParams, bean, null, null)
    verify(inputParams).setAssigneeId("fred2")
  }


  @Test def shouldDoNothingWhenMappingIsNotFoundAndUnassignedAreAllowed(){
    val bean = new CopyIssueBean
    bean.setAssignee(userBean)

    plugMapMethod(new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND))
    when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(true)

    asigneeFieldMapper.populateInputParameters(inputParams, bean, null, project)
    verify(inputParams, never()).setAssigneeId(Matchers.any())
  }



  def plugMapMethod(result : InternalMappingResult) {
    pluggedMappingResult = result
    plugedMapMethod = true
  }


  def mapAndTest(desiredUser : User, desiredDecision : InternalMappingResult.MappingResultDecision, userBean : UserBean = this.userBean){
    val result = asigneeFieldMapper.mapUser(userBean, project)
    assertEquals(desiredUser, result.mappedUser)
    assertEquals(desiredDecision, result.decision)
  }

}
