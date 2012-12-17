package com.atlassian.cpji.tests.pageobjects

import scala.collection.JavaConverters._
import com.atlassian.jira.pageobjects.dialogs.FormDialog
import org.openqa.selenium.By
import com.atlassian.pageobjects.elements.{ElementBy, PageElement}
import com.atlassian.jira.pageobjects.components.fields.{QueryableDropdownSelect, AutoComplete}

class IssueActionsDialog extends FormDialog("issue-actions-dialog") {
	private final val ISSUE_ACTIONS_QUERYABLE_CONTAINER: By = By.id("issueactions-queryable-container")
	private final val ISSUE_ACTIONS: By = By.id("issueactions-suggestions")

	@ElementBy(id = "issueactions-suggestions") protected var suggestions: PageElement = null

	def getAutoComplete: AutoComplete = {
		return binder.bind(classOf[QueryableDropdownSelect], ISSUE_ACTIONS_QUERYABLE_CONTAINER, ISSUE_ACTIONS)
	}

	def getActionsLinksByQuery(query: String): Iterable[String] = {
		getAutoComplete.query(query)
		return suggestions.findAll(By.tagName("a")).asScala.map(_.getAttribute("href"))
	}

	def queryAndSelect[T](search: String, dialogClass: Class[T], args: AnyRef*): T = {
		getAutoComplete.query(search).getActiveSuggestion.click
		return binder.bind(dialogClass, args)
	}
}
