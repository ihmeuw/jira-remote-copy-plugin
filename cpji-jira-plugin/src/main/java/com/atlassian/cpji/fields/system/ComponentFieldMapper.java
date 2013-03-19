package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.ComponentBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.ComponentsSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class ComponentFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final ProjectComponentManager projectComponentManager;

    public ComponentFieldMapper(ProjectComponentManager projectComponentManager, final Field field)
    {
        super(field);
        this.projectComponentManager = projectComponentManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return ComponentsSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        List<ComponentBean> components = makeSureNotNull(bean.getComponents());
        List<Long> componentIds = new ArrayList<Long>();
        for (ComponentBean component : components)
        {
            Long componentId = findProjectComponent(component.getName(), project.getId());
            if (componentId != null)
            {
                componentIds.add(componentId);
            }
        }
        Long[] ids = new Long[componentIds.size()];
        componentIds.toArray(ids);
        inputParameters.setComponentIds(ids);
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedFieldValues = new ArrayList<String>();
        List<ComponentBean> components = makeSureNotNull(bean.getComponents());
        if (components.isEmpty())
        {
             return new MappingResult(unmappedFieldValues, false, true, defaultValueConfigured(project, bean));
        }
        boolean hasValidValue = false;
        for (ComponentBean component : components)
        {
            Long componentId = findProjectComponent(component.getName(), project.getId());
            if (componentId == null)
            {
                unmappedFieldValues.add(component.getName());
            }
            else
            {
                hasValidValue = true;
            }
        }
        return new MappingResult(unmappedFieldValues, hasValidValue, false, defaultValueConfigured(project, bean));
    }

    private Long findProjectComponent(final String name, final long projectId)
    {
        Collection<ProjectComponent> projectComponents = projectComponentManager.findAllForProject(projectId);
        try
        {
            ProjectComponent projectComponent = Iterables.find(projectComponents, new Predicate<ProjectComponent>()
            {
                public boolean apply(final ProjectComponent input)
                {
                    return (name.equals(input.getName()));
                }
            });
            return projectComponent.getId();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    private <T> List makeSureNotNull(List<T> inputList)
    {
        return (inputList == null) ? Lists.newArrayList() : inputList;
    }
}
