package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.IssueTypeSystemField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class IssueTypeFieldMapper extends AbstractFieldMapper  implements SystemFieldIssueCreationFieldMapper
{
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    public IssueTypeFieldMapper(IssueTypeSchemeManager issueTypeSchemeManager, final FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.ISSUE_TYPE), defaultFieldValuesManager);
        this.issueTypeSchemeManager = issueTypeSchemeManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return IssueTypeSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        final IssueType issueType = findIssueType(bean, project);
        inputParameters.setIssueTypeId(issueType.getId());
    }

    public String getFieldId()
    {
        return IssueFieldConstants.ISSUE_TYPE;
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        final IssueType issueType = findIssueType(bean, project);
        if (issueType == null)
        {
            return new MappingResult(Lists.newArrayList(bean.getTargetIssueType()), false, false, hasDefaultValue(
					project, bean));
        }
        return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
    }

    private IssueType findIssueType(final CopyIssueBean copyIssueBean, final Project project)
    {
        Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
        try
        {
            return Iterables.find(issueTypesForProject, new Predicate<IssueType>()
            {
                public boolean apply(final IssueType input)
                {
                    return input.getName().equals(copyIssueBean.getTargetIssueType());
                }
            });
        }
        catch (NoSuchElementException ex)
        {
            throw new RuntimeException("No issue type with name '" + copyIssueBean.getTargetIssueType() + "' found!");
        }
    }
}
