package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.VersionCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class VersionCFMapper extends AbstractMultiValueCFMapper<Version>
{
	private final VersionManager versionManager;

	public VersionCFMapper(final VersionManager versionManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		super(defaultFieldValuesManager);
		this.versionManager = versionManager;
    }

    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof VersionCFType;
    }

    @Override
    protected String convertToString(final Version value)
    {
        return value.getName();
    }

    @Override
    protected String formatStringForInputParams(final String value, final CustomField customField, final Project project, IssueType issueType)
    {
		return versionManager.getVersion(project.getId(), value).getId().toString();
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return versionManager.getVersion(project.getId(), value) != null;
    }
}
