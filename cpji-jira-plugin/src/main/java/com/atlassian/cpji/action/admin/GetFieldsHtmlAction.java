package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;

/**
 * @since v2.1
 */
public class GetFieldsHtmlAction extends RequiredFieldsAwareAction
{

    public GetFieldsHtmlAction(FieldLayoutItemsRetriever fieldLayoutItemsRetriever, IssueTypeSchemeManager issueTypeSchemeManager, final IssueFactory issueFactory, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(fieldLayoutItemsRetriever, issueTypeSchemeManager, issueFactory, defaultFieldValuesManager);
    }

}
