package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.model.IssueOperation;
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


    public ExtendedViewIssuePage(String issueKey) {
        super(issueKey);
    }


    public void invokeRIC(){
        getIssueMenu().invoke(new RemoteCopyOperation());
    }

    public void invokeClone(){
        getIssueMenu().invoke(new CloneOperation());
    }

    public boolean hasRIC() {
        return getIssueMenu().isItemPresentInMoreActionsMenu(new RemoteCopyOperation().uiName());
    }

    public boolean hasClone() {
        return getIssueMenu().isItemPresentInMoreActionsMenu(new CloneOperation().uiName());
    }

    public IssueActionsDialog openDOTSection(){
        pageElementFinder.find(By.tagName("body")).type(".");
        return pageBinder.bind(IssueActionsDialog.class);
    }

    static class RemoteCopyOperation implements IssueOperation
    {
        @Override
        public String id()
        {
            return "clone-issue-ric";
        }

        @Override
        public String uiName()
        {
            return "Remote Copy";
        }

        @Override
        public String cssClass()
        {
            return "issueaction-clone-issue ";
        }

        @Override
        public boolean hasShortcut()
        {
            return false;
        }

        @Override
        public CharSequence shortcut()
        {
            return null;
        }
    }

    static class CloneOperation implements IssueOperation
    {
        @Override
        public String id()
        {
            return "clone-issue";
        }

        @Override
        public String uiName()
        {
            return "Clone";
        }

        @Override
        public String cssClass()
        {
            return "issueaction-clone-issue ";
        }

        @Override
        public boolean hasShortcut()
        {
            return false;
        }

        @Override
        public CharSequence shortcut()
        {
            return null;
        }
    }


}
