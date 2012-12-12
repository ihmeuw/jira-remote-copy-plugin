package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class RequiredFieldsAwareAction extends JiraWebActionSupport implements OperationContext
{
	public static final ArrayList<String> UNMODIFIABLE_FIELDS = Lists.newArrayList(IssueFieldConstants.ISSUE_TYPE, IssueFieldConstants.PROJECT, IssueFieldConstants.SUMMARY);

    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final IssueFactory issueFactory;
    private final DefaultFieldValuesManager defaultFieldValuesManager;

    private String projectKey;
    private String issuetype;
    private MutableIssue issue;
    private Map fieldValuesHolder;

    public RequiredFieldsAwareAction(FieldLayoutItemsRetriever fieldLayoutItemsRetriever, IssueTypeSchemeManager issueTypeSchemeManager, final IssueFactory issueFactory, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.issueFactory = issueFactory;
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.fieldValuesHolder = new HashMap();
    }

    public String getHtmlForField(FieldLayoutItem fieldLayoutItem)
    {
        OrderableField orderableField = fieldLayoutItem.getOrderableField();
        Object defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(getProject().getKey(), orderableField.getId(), getIssueTypeObject().getName());
        if (ActionContext.getParameters().get(orderableField.getId()) == null)
        {
            if (defaultFieldValue != null)
            {
                Map actionParams = new HashMap();
                actionParams.put(orderableField.getId(), defaultFieldValue);
                orderableField.populateFromParams(getFieldValuesHolder(), actionParams);
            }
        }

        return orderableField.getEditHtml(fieldLayoutItem, this, this, getIssue(), getDisplayParameters());
    }


    public List<FieldLayoutItem> getFieldLayoutItems()
    {
        Iterable<FieldLayoutItem> filter = Iterables.filter(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(getProject(), getIssueTypeObject()), new Predicate<FieldLayoutItem>()
        {
            public boolean apply(final FieldLayoutItem input)
            {
                return !UNMODIFIABLE_FIELDS.contains(input.getOrderableField().getId()) && input.isRequired();
            }
        });
        return Lists.newArrayList(filter);
    }

    public Map getFieldValuesHolder()
    {
        return fieldValuesHolder;
    }

    @Override
    public IssueOperation getIssueOperation()
    {
        return IssueOperations.EDIT_ISSUE_OPERATION;
    }

    public static Map getDisplayParameters()
    {
        Map displayParameters = new HashMap();
        displayParameters.put("theme", "aui");
        return displayParameters;
    }

    protected MutableIssue getIssue()
    {
        if (issue == null)
        {
            issue = issueFactory.getIssue();
        }
        issue.setProjectId(getProject().getId());
        issue.setIssueTypeId(getIssueTypeObject().getId());
        return issue;
    }

    protected Project getProject()
    {
        if (StringUtils.isEmpty(projectKey))
        {
            return null;
        }
        return getProjectManager().getProjectObjByKey(projectKey);
    }


    protected IssueType getIssueTypeObject()
    {
        try
        {
            return Iterables.find(issueTypeSchemeManager.getIssueTypesForProject(getProject()), new Predicate<IssueType>()
            {

                public boolean apply(final IssueType input)
                {
                    return input.getId().equals(getIssuetype());
                }
            });
        }
        catch (NoSuchElementException ex)
        {
            return null;
        }

    }

    public void setProjectKey(final String projectKey)
    {
        this.projectKey = projectKey;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public String getIssuetype()
    {
        if(issuetype == null){
            issuetype = Iterables.get(issueTypeSchemeManager.getIssueTypesForProject(getProject()), 0).getId();
        }
        return issuetype;
    }

    public void setIssuetype(final String issuetype)
    {
        this.issuetype = issuetype;
    }
}
