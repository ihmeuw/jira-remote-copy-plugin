package com.atlassian.cpji.fields.custom;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.issue.customfields.impl.MultiGroupCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.groups.GroupManager;

/**
 * Maps the {@link MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class MultiGroupCFMapper extends AbstractMultiValueCFMapper<Group>
{
    private final GroupManager groupManager;

    public MultiGroupCFMapper(final GroupManager groupManager)
    {
        this.groupManager = groupManager;
    }

    @Override
    public String getType()
    {
        return MultiGroupCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final Group value)
    {
        return value.getName();
    }

    @Override
    protected String formatString(final String value)
    {
        return value;
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return groupManager.groupExists(value);
    }
}