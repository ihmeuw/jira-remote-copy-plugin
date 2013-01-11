package com.atlassian.cpji.fields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
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
	private final FieldScreenManager fieldScreenManager;

	public FieldLayoutItemsRetriever(final FieldManager fieldManager, FieldLayoutManager fieldLayoutManager, final FieldScreenManager fieldScreenManager)
    {
        this.fieldManager = fieldManager;
        this.fieldLayoutManager = fieldLayoutManager;
		this.fieldScreenManager = fieldScreenManager;
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
				// field is not associated with any screen or tab, create issue will not pick up its value so hide it
				if (fieldScreenManager.getFieldScreenTabs(input.getOrderableField().getId()).isEmpty()) {
					return false;
				}

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

    public FieldLayoutItem getIssueTypeField(final Project project){
        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, null);
        return fieldLayout.getFieldLayoutItem(IssueFieldConstants.ISSUE_TYPE);
    }

}
