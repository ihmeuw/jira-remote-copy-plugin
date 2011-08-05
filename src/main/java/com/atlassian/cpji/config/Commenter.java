package com.atlassian.cpji.config;

import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ErrorCollection;

/**
 * TODO: Document this class / interface here
 *
 * @since v4.4
 */
public class Commenter
{
    private CommentService commentService;
    private CopyIssueConfigurationManager copyIssueConfigurationManager;
    private JiraAuthenticationContext jiraAuthenticationContext;

    public Commenter(CommentService commentService, CopyIssueConfigurationManager copyIssueConfigurationManager, JiraAuthenticationContext jiraAuthenticationContext)
    {
        this.commentService = commentService;
        this.copyIssueConfigurationManager = copyIssueConfigurationManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public void addCommentToIssue(Issue issue, String comment, ErrorCollection errorCollection)
    {
        CommentSecurityLevel commentSecurityLevel = copyIssueConfigurationManager.getCommentSecurityLevel(issue.getProjectObject());
        if (commentSecurityLevel != null)
        {
            if (commentSecurityLevel.isGroupLevel())
            {
                commentService.create(jiraAuthenticationContext.getLoggedInUser(), issue, comment, commentSecurityLevel.getId(), null, false, errorCollection);
            }
            else
            {
                commentService.create(jiraAuthenticationContext.getLoggedInUser(), issue, comment, null, Long.valueOf(commentSecurityLevel.getId()), false, errorCollection);
            }
        }
        else
        {
            commentService.create(jiraAuthenticationContext.getLoggedInUser(), issue, comment, false, errorCollection);
        }
    }


}
