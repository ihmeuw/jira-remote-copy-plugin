package com.atlassian.cpji

import com.atlassian.cpji.tests.pageobjects.{SelectTargetProjectPage, CopyDetailsPage}
import com.atlassian.jira.pageobjects.JiraTestedProduct

object CopyIssueProcess {

  def goToCopyDetails(jira: JiraTestedProduct, issueId : java.lang.Long, project : String = "Blah") : CopyDetailsPage = {
    return jira
      .visit(classOf[SelectTargetProjectPage], issueId)
      .setDestinationProject(project)
      .next()
  }

}
