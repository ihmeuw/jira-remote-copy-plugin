package com.atlassian.cpji.fields.custom;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.util.UserManager;

/**
 * Maps the {@link com.atlassian.jira.issue.customfields.impl.MultiGroupCFType} custom field type.
 *
 * @since v2.1
 */
public class MultiUserCFMapper extends AbstractMultiValueCFMapper<User>
{

	private final UserManager userManager;

	public MultiUserCFMapper(final UserManager userManager)
    {
		this.userManager = userManager;
	}

    @Override
    public String getType()
    {
        return MultiUserCFType.class.getCanonicalName();
    }

    @Override
    protected String convertToString(final User value)
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
        return userManager.getUser(value) != null;
    }
}
