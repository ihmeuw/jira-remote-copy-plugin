package com.atlassian.cpji.config;

import com.atlassian.jira.project.Project;

import java.util.List;

/**
 * @since v2.0
 */
public interface CopyIssueConfigurationManager
{
    public void setSecurityLevel(CommentSecurityLevel issueSecurityLevel, final Project project);

    public void clearCommentSecurityLevel(Project project);

    public List<CommentSecurityLevel> getSecurityLevels(Project project);

    public CommentSecurityLevel getCommentSecurityLevel(final Project project);

    UserMappingType getUserMappingType();
}
