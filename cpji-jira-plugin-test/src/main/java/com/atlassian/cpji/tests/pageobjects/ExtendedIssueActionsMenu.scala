package com.atlassian.cpji.tests.pageobjects

import com.atlassian.jira.pageobjects.components.menu.IssueActionsMenu
import org.openqa.selenium.By
import scala.collection.JavaConverters._

class ExtendedIssueActionsMenu(issueId: java.lang.Long) extends IssueActionsMenu(By.id("actions_" + issueId), By.id("actions_" + issueId + "_drop")) {


	def getActionLinks(): Iterable[String] = {
		getDropdown.findAll(By.className("aui-list-item-link")).asScala.map(_.getAttribute("href"))
	}

}
