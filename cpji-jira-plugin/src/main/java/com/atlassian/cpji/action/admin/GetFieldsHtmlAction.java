package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManagerImpl;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.security.Permissions;

/**
 * @since v2.1
 */
public class GetFieldsHtmlAction extends RequiredFieldsAwareAction
{

    public GetFieldsHtmlAction(FieldLayoutItemsRetriever fieldLayoutItemsRetriever, IssueTypeSchemeManager issueTypeSchemeManager, final IssueFactory issueFactory, final DefaultFieldValuesManagerImpl defaultFieldValuesManager)
    {
        super(fieldLayoutItemsRetriever, issueTypeSchemeManager, issueFactory, defaultFieldValuesManager);
    }

    protected boolean isPermissionDenied() {
        return !getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser())
                && !getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, getProject(), getLoggedInUser());
    }

    @Override
    protected String doExecute() throws Exception
    {
        if (isPermissionDenied())
        {
            return PERMISSION_VIOLATION_RESULT;
        }

        return super.doExecute();
    }
}
