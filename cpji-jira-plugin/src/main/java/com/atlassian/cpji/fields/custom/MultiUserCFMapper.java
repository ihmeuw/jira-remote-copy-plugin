package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class MultiUserCFMapper extends AbstractMultiValueCFMapper<ApplicationUser>
{

	private final UserManager userManager;

	public MultiUserCFMapper(final UserManager userManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		super(defaultFieldValuesManager);
		this.userManager = userManager;
	}

    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof MultiUserCFType;
    }

    @Override
    protected String convertToString(final ApplicationUser value)
    {
        return value.getName();
    }

    @Override
	protected String formatStringForInputParams(String value, CustomField customField, Project project, IssueType issueType)
    {
        return value;
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return userManager.getUserByName(value) != null;
    }

    @Override
    protected ApplicationUser convertToGenericType(final Object value)
    {
        if(value == null)
            return null;

        return (ApplicationUser) value;
    }

}
