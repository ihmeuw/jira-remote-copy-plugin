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
            return "copy-issue-to-other-instance";
        }

        @Override
        public String uiName()
        {
            return "Remote Copy";
        }

        @Override
        public String cssClass()
        {
            return "issueaction-movie-issue";
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

    @ElementBy(id="copy-issue-to-other-instance")
    protected PageElement ricCloneIssueAction;

    public TimedCondition hasDefaultCloneAction(){
        return cloneIssueAction.timed().isPresent();
    }

    public TimedCondition hasRICCloneAction(){
        return ricCloneIssueAction.timed().isPresent();
    }

    public IssueOperation getRICOperation(){
        return new RemoteCopyOperation();

    }

}
