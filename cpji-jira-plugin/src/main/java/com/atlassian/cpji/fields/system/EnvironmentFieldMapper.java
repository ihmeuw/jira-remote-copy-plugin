package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.EnvironmentSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;

/**
 * @since v1.4
 */
public class EnvironmentFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    public EnvironmentFieldMapper(final Field field)
    {
        super(field);
    }

    public Class<? extends OrderableField> getField()
    {
        return EnvironmentSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        inputParameters.setEnvironment(bean.getEnvironment());
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        if (bean.getEnvironment() != null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, false);
        }
        else
        {
            return new MappingResult(Collections.<String>emptyList(), false, true);
        }
    }
}
