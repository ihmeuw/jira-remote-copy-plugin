package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * @since v3.0
 */
public class ExtendedViewIssuePage extends ViewIssuePage {

    @Inject
    protected PageBinder pageBinder;

    @Inject
    protected PageElementFinder pageElementFinder;

    @Inject
    protected IssueActionsFragment issueActionsFragment;





    public ExtendedViewIssuePage(String issueKey) {
        super(issueKey);
    }

    @Init
    public void init()
    {
        issueActionsFragment = pageBinder.bind(IssueActionsFragment.class);
    }

    public IssueActionsFragment getIssueActionsFragment() {
        return issueActionsFragment;
    }

    public void invokeRIC(){
        getIssueMenu().invoke(issueActionsFragment.getRICOperation());
    }

    public void invokeClone(){
        getIssueMenu().invoke(issueActionsFragment.getCloneOperation());
    }

    public IssueActionsDialog openDOTSection(){
        pageElementFinder.find(By.tagName("body")).type(".");
        return pageBinder.bind(IssueActionsDialog.class);
    }
}
