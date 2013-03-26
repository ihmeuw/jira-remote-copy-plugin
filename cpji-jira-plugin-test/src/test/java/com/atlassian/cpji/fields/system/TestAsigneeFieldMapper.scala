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
import com.atlassian.jira.issue.IssueFieldConstants
import com.atlassian.cpji.fields.value.UserMappingManager
import com.atlassian.cpji.rest.model.UserBean
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.security.{PermissionManager, Permissions}

@RunWith(classOf[MockitoJUnitRunner]) class TestAsigneeFieldMapper {


  var asigneeFieldMapper : AssigneeFieldMapper = null

  //components
  @Mock var userMappingManager : UserMappingManager = null
  @Mock var permissionManager : PermissionManager = null

  //data objects
  @Mock var project : Project = null
  @Mock var userBean :UserBean = null
  @Mock var projectLead : User = null
  @Mock var mappedUser : User = null

  @Before def setUp {
    val fieldManager = mock(classOf[FieldManager])
    val assigneeField = mock(classOf[OrderableField])
    when(fieldManager.getField(IssueFieldConstants.ASSIGNEE)).thenReturn(assigneeField)

    when(project.getLead).thenReturn(projectLead)

    asigneeFieldMapper = new AssigneeFieldMapper(permissionManager, null, fieldManager, userMappingManager, null)
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


  def mapAndTest(desiredUser : User, desiredDecision : InternalMappingResult.MappingResultDecision, userBean : UserBean = this.userBean){
    val result = asigneeFieldMapper.mapUser(userBean, project)
    assertEquals(desiredUser, result.mappedUser)
    assertEquals(desiredDecision, result.decision)
  }

}
