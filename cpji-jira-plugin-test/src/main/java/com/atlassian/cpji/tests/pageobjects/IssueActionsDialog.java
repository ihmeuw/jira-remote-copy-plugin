package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.components.fields.AutoComplete;
import com.atlassian.jira.pageobjects.components.fields.QueryableDropdownSelect;
import com.atlassian.jira.pageobjects.dialogs.FormDialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;

import javax.annotation.Nullable;

/**
 * @since v3.0
 */
public class IssueActionsDialog extends FormDialog {
    public IssueActionsDialog() {
        super("issue-actions-dialog");
    }

    private final By ISSUE_ACTIONS_QUERYABLE_CONTAINER = By.id("issueactions-queryable-container");
    private final By ISSUE_ACTIONS = By.id("issueactions-suggestions");

    @ElementBy(id = "issueactions-suggestions")
    protected PageElement suggestions;

    public AutoComplete getAutoComplete() {
        return binder.bind(QueryableDropdownSelect.class, ISSUE_ACTIONS_QUERYABLE_CONTAINER, ISSUE_ACTIONS);
    }


    public Iterable<String> getActionsLinksByQuery(String query) {
        getAutoComplete().query(query);
        return Iterables.transform(suggestions.findAll(By.tagName("a")), new Function<PageElement, String>() {
            @Override
            public String apply(@Nullable PageElement input) {
                return input.getAttribute("href");
            }
        });
    }

}
