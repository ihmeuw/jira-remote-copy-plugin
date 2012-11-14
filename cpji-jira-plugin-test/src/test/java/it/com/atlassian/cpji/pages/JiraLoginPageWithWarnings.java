package it.com.atlassian.cpji.pages;

import com.atlassian.jira.pageobjects.elements.AuiMessage;
import com.atlassian.jira.pageobjects.elements.AuiMessages;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.collect.Iterables;

/**
 * @since v2.1
 */
public class JiraLoginPageWithWarnings extends JiraLoginPage {
	public Iterable<PageElement> getWarnings()
	{
		return Iterables.filter(messages, AuiMessages.isAuiMessageOfType(AuiMessage.Type.WARNING));
	}

	public boolean hasWarnings()
	{
		return !Iterables.isEmpty(getWarnings());
	}
}