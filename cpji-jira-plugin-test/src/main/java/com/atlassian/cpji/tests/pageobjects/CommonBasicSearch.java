package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.AbstractTimedCondition;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;

import javax.inject.Inject;

/**
 * This page object represents basic search page. Unforutnately it is not possible to use {@link com.atlassian.jira.pageobjects.navigator.BasicSearch}
 * due to issenav plugin which works in different way
 * @since v3.0
 */
public class CommonBasicSearch extends AbstractJiraPage {

    @ElementBy(className = "navigator-content")
    protected PageElement kaNavigatorContent;

    @ElementBy (id = "issue-filter-submit")
    protected PageElement search;

    @Inject
    protected Timeouts timeouts;


    @Override
    public TimedCondition isAt() {
        return new AbstractTimedCondition(timeouts.timeoutFor(TimeoutType.DEFAULT),
                timeouts.timeoutFor(TimeoutType.EVALUATION_INTERVAL)) {
            @Override
            protected Boolean currentValue() {
                return (kaNavigatorContent.isPresent() && kaNavigatorContent.isVisible()) || (search.isPresent() && search.isVisible());
            }
        };

    }

    @Override
    public String getUrl() {
        return "/secure/IssueNavigator.jspa?reset=true&mode=show&summary=true&description=true&body=true&query=";
    }

}
