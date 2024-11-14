package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.UserCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.NumberCFType} custom field type.
 *
 * @since v2.1
 */
public class UserCFMapper extends AbstractSingleValueCFMapper<ApplicationUser>
{
	private final UserManager userManager;

	public UserCFMapper(final UserManager userManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		super(defaultFieldValuesManager);
		this.userManager = userManager;
	}

    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof UserCFType;
    }

    @Override
    protected String convertToString(final ApplicationUser value)
    {
        return value.getName();
    }

    @Override
    protected String formatString(final String value, final CustomField customField, final Project project, final IssueType issueType)
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
