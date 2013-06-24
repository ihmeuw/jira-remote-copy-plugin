package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.ComponentBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.ComponentsSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class ComponentFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final ProjectComponentManager projectComponentManager;

    public ComponentFieldMapper(ProjectComponentManager projectComponentManager, final FieldManager fieldManager,
			final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.COMPONENTS), defaultFieldValuesManager);
        this.projectComponentManager = projectComponentManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return ComponentsSystemField.class;
    }

	@Override
	public void populateInputParams(CachingUserMapper userMapper, IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
		MappingResult mappingResult = getMappingResult(userMapper, copyIssueBean, project);
		if (!mappingResult.hasOneValidValue() && fieldLayoutItem.isRequired()) {
			String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), getFieldId(), issueType.getName());
			if (defaultFieldValue != null) {
				inputParameters.getActionParameters().put(getFieldId(), defaultFieldValue);
			}
		} else {
			populateCurrentValue(inputParameters, copyIssueBean, fieldLayoutItem, project);
		}
	}

    public void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        final List<ComponentBean> components = makeSureNotNull(bean.getComponents());
        final List<Long> componentIds = new ArrayList<Long>();
        for (ComponentBean component : components)
        {
            Long componentId = findProjectComponent(component.getName(), project.getId());
            if (componentId != null)
            {
                componentIds.add(componentId);
            }
        }
		if (componentIds.size() > 0) {
			Long[] ids = new Long[componentIds.size()];
			componentIds.toArray(ids);
			inputParameters.setComponentIds(ids);
		}
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedFieldValues = new ArrayList<String>();
        List<ComponentBean> components = makeSureNotNull(bean.getComponents());
        if (components.isEmpty())
        {
             return new MappingResult(unmappedFieldValues, false, true, hasDefaultValue(project, bean));
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
        return new MappingResult(unmappedFieldValues, hasValidValue, false, hasDefaultValue(project, bean));
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
