package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.jira.issue.fields.OrderableField;

/**
 * @since v4.4
 */
public interface SystemFieldIssueCreationFieldMapper extends IssueCreationFieldMapper
{
    String getFieldId();

    Class<? extends OrderableField>  getField();
}
