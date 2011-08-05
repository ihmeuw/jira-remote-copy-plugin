package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.PrioritySystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class PriorityFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final ConstantsManager constantsManager;

    public PriorityFieldMapper(ConstantsManager constantsManager, final Field field)
    {
        super(field);
        this.constantsManager = constantsManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return PrioritySystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        Priority priority = findPriority(bean.getPriority());
        inputParameters.setPriorityId(priority.getId());
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        if (StringUtils.isEmpty(bean.getPriority()))
        {
           return new MappingResult(Collections.<String>emptyList(), false, true);
        }
        Priority priority = findPriority(bean.getPriority());
        if (priority == null)
        {
           return new MappingResult(Lists.newArrayList(bean.getPriority()), false, false);
        }
        return new MappingResult(Collections.<String>emptyList(), true, false);
    }

    private Priority findPriority(final String priority)
    {
        try
        {
            return Iterables.find(constantsManager.getPriorityObjects(), new Predicate<Priority>()
            {
                public boolean apply(final Priority input)
                {
                    return input.getName().equals(priority);
                }
            });
        }
        catch (NoSuchElementException ex)
        {
            return constantsManager.getDefaultPriorityObject();
        }
    }
}
