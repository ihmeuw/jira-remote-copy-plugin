package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.LabelsCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.project.Project;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class LabelsCFMapper extends AbstractMultiValueCFMapper<Label>
{
	public LabelsCFMapper(final DefaultFieldValuesManager defaultFieldValuesManager) {
		super(defaultFieldValuesManager);
	}

	@Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof LabelsCFType;
    }

    @Override
    protected String convertToString(final Label value)
    {
        return value.getLabel();
    }

    @Override
	protected String formatStringForInputParams(String value, CustomField customField, Project project, IssueType issueType)
    {
        return value;
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return true; // labels are always valid
    }
}
