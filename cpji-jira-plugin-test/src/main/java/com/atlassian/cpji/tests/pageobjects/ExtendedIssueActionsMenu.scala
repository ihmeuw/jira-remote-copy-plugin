package com.atlassian.cpji.tests.pageobjects

import com.atlassian.jira.pageobjects.components.menu.IssueActionsMenu
import org.openqa.selenium.By
import scala.collection.JavaConverters._
import com.atlassian.pageobjects.elements.query.{ExpirationHandler, AbstractTimedQuery, Conditions, TimedQuery}
import com.atlassian.pageobjects.elements.timeout.{TimeoutType, Timeouts}
import javax.inject.Inject
import com.google.common.collect.Lists

class ExtendedIssueActionsMenu(issueId: java.lang.Long) extends IssueActionsMenu(By.id("actions_" + issueId), By.id("actions_" + issueId + "_drop")) {

	@Inject
	var timeouts : Timeouts = null

	def getActionLinks(): TimedQuery[java.lang.Iterable[java.lang.String]] = {
		makeQuery(timeouts, _ => Lists.newArrayList(getDropdown.findAll(By.className("aui-list-item-link")).asScala.map(_.getAttribute("href")).asJava))
	}

	def clickActionByName[P](name: String) {
		getDropdown.findAll(By.className("aui-list-item-link")).asScala.find(_.getText.equalsIgnoreCase(name)).get.click()
	}

	def  makeQuery[T](timouets : Timeouts, func : Unit => T) = new AbstractTimedQuery[T](timouets.timeoutFor(TimeoutType.DEFAULT),
		timouets.timeoutFor(TimeoutType.EVALUATION_INTERVAL), ExpirationHandler.RETURN_CURRENT) {
		def shouldReturn(currentEval: T): Boolean = true

		def currentValue(): T = func(Unit)
	}

}
