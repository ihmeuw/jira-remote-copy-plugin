package com.atlassian.cpji.fields;

import com.atlassian.jira.issue.IssueFieldConstants;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * @since v2.0
 */
public class MappingConstants
{
    private MappingConstants()
    {
    }

    /**
     * This is a list of systems field ids which are not having an implementation of {@link com.atlassian.jira.issue.fields.OrderableField}
     */
    public static final List<String> nonSystemFieldsFieldIds = Lists.newArrayList(IssueFieldConstants.WATCHERS, IssueFieldConstants.VOTERS);
}
