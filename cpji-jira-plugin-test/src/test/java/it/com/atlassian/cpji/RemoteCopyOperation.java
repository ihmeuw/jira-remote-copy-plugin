package it.com.atlassian.cpji;


import com.atlassian.jira.pageobjects.model.IssueOperation;

/**
 * Represents the Remote Copy operation in the More Actions menu.
 *
 * TODO currently the ViewIssuePage does not support accepting IssueOperations.
 *
 * @since v2.1
 */
public class RemoteCopyOperation implements IssueOperation
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
