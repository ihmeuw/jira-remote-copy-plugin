package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.ProjectSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;

/**
 * @since v1.4
 */
public class ProjectSystemFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    public ProjectSystemFieldMapper(final Field field)
    {
        super(field);
    }

    public Class<? extends OrderableField> getField()
    {
        return ProjectSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
         inputParameters.setProjectId(project.getId());
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        return new MappingResult(Collections.<String>emptyList(), true, false, defaultValueConfigured(project, bean));
    }

}
