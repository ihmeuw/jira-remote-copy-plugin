package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.model.IssueOperation;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;

/**
 * @since v3.0
 */
public class IssueActionsFragment {

    /**
     * Represents the Remote Copy operation in the More Actions menu.
     *
     * TODO currently the ViewIssuePage does not support accepting IssueOperations.
     *
     * @since v2.1
     */
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



    @ElementBy(className="issueaction-clone-issue")
    protected PageElement cloneIssueAction;

    @ElementBy(id="clone-issue-ric")
    protected PageElement hasRICCloneIssueAction;


    public TimedCondition hasCloneAction(){
        return cloneIssueAction.timed().isPresent();
    }

    public TimedCondition hasRICCloneAction(){
        return hasRICCloneIssueAction.timed().isPresent();
    }

    public IssueOperation getRICOperation(){
        return new RemoteCopyOperation();
    }

    public IssueOperation getCloneOperation(){
        return new CloneOperation();
    }


}
