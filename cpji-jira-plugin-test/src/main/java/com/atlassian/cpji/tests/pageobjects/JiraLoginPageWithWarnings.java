package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.elements.AuiMessage;
import com.atlassian.jira.pageobjects.elements.AuiMessages;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.google.common.collect.Iterables;

/**
 * @since v2.1
 */
public class JiraLoginPageWithWarnings extends AbstractJiraPage {
    @ElementBy(id = "login-form")
    protected PageElement loginForm;

	@ElementBy(className = "aui-message", within = "loginForm")
	protected Iterable<PageElement> messages;

	public Iterable<PageElement> getWarnings()
	{
		return Iterables.filter(messages, AuiMessages.isAuiMessageOfType(AuiMessage.Type.WARNING));
	}

    public boolean hasWarnings()
	{
		return !Iterables.isEmpty(getWarnings());
	}

    @Override
    public TimedCondition isAt() {
        return loginForm.timed().isPresent();
    }

    @Override
    public String getUrl() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
