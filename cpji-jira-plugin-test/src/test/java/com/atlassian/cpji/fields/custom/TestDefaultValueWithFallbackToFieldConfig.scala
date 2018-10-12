package com.atlassian.cpji.fields.custom

import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.Mockito._
import org.junit.{Before, Test}
import org.mockito.{ArgumentMatchers, Mock}
import com.atlassian.jira.project.Project
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.mock.MockitoSugar
import com.atlassian.jira.project.MockProject
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.issue.context.IssueContextImpl
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.customfields.CustomFieldType
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

import collection.JavaConverters._

@RunWith(classOf[MockitoJUnitRunner])
class TestDefaultValueWithFallbackToFieldConfig extends ShouldMatchersForJUnit with MockitoSugar {

  var defaultValueStrategy : DefaultValueWithFallbackToFieldConfig = null

  @Mock var defaultValuesManager : DefaultFieldValuesManager = null
  val project : Project  =  new MockProject(123L, "PRJ", "Project ppp")
  @Mock var customField : CustomField = null
  @Mock var issueType : IssueType = null


  @Before def setUp {
    defaultValueStrategy = new DefaultValueWithFallbackToFieldConfig(defaultValuesManager)

    when(customField.getId).thenReturn("CF123")
    when(issueType.getName).thenReturn("IssueTypeName")
  }



  @Test def getDefaultShouldTryToFindValueFromManager(){
    val defVals = Array("123", "456")
    when(defaultValuesManager.getDefaultFieldValue(project.getKey, customField.getId, issueType.getName)).thenReturn(defVals)

    val result = defaultValueStrategy.getDefaultValue(customField, project, issueType)

    result should equal(defVals)
  }


  @Test def getDefaultShouldTryToGetDefaultValueFromConfigWhenNoInnerDefValIsSet(){
    when(defaultValuesManager.getDefaultFieldValue(project.getKey, customField.getId, issueType.getName)).thenReturn(null)

    val fieldConfig = mock[FieldConfig]
    val customFieldType = mock[CustomFieldType[_, _]]
    when(customField.getRelevantConfig(new IssueContextImpl(project, issueType))).thenReturn(fieldConfig)
    doReturn(customFieldType, customFieldType).when(customField).getCustomFieldType

    val defVals = Array("this", "is", "default", "value")

    doReturn(defVals.toList.asJava, defVals.toList.asJava).when(customFieldType).getDefaultValue(fieldConfig)
    when(customFieldType.getStringFromSingularObject(ArgumentMatchers.any())).thenAnswer(new Answer[String] {
      def answer(invocation: InvocationOnMock): String = {
        val args: Array[AnyRef] = invocation.getArguments
        args(0).asInstanceOf[String]

      }
    })

    val result = defaultValueStrategy.getDefaultValue(customField, project, issueType)
    result should equal(defVals)

  }
}
