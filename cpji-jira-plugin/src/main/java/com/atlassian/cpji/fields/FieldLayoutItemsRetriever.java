package com.atlassian.cpji.fields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @since v4.4
 */
public class FieldLayoutItemsRetriever
{
    private final FieldManager fieldManager;
    private final FieldLayoutManager fieldLayoutManager;

    public FieldLayoutItemsRetriever(final FieldManager fieldManager, FieldLayoutManager fieldLayoutManager)
    {
        this.fieldManager = fieldManager;
        this.fieldLayoutManager = fieldLayoutManager;
    }

    public Iterable<FieldLayoutItem> getAllVisibleFieldLayoutItems(final Issue issue)
    {
        return getAllVisibleFieldLayoutItems(issue.getProjectObject(), issue.getIssueTypeObject());
    }

    public Iterable<FieldLayoutItem> getAllVisibleFieldLayoutItems(final Project project, final IssueType issueType)
    {
        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());
        return Iterables.filter(fieldLayout.getFieldLayoutItems(), new Predicate<FieldLayoutItem>()
        {
            public boolean apply(final FieldLayoutItem input)
            {
                if (fieldManager.isCustomField(input.getOrderableField()))
                {
                    CustomField customField = (CustomField) input.getOrderableField();
                    boolean inScope = customField.isInScope(project, Lists.newArrayList(issueType.getName()));
                    return !input.isHidden() && inScope;
                }
                return !input.isHidden();
            }
        });
    }
}
