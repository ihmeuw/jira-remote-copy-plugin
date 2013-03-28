package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.jira.issue.Issue;

/**
 * @since v1.4
 */
public interface PostIssueCreationFieldMapper extends FieldMapper
{
    void process(CachingUserMapper userMapper, Issue issue, final CopyIssueBean bean) throws FieldCreationException;
}
