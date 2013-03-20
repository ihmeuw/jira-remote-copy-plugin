package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.SummarySystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 *
 * @since v1.4
 */
public class SummaryFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    public SummaryFieldMapper(FieldManager fieldManager, DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.SUMMARY), defaultFieldValuesManager);
    }

    public Class<? extends OrderableField> getField()
    {
        return SummarySystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        inputParameters.setSummary(bean.getSummary());
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        if (bean.getSummary() != null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, false, defaultValueConfigured(project, bean));
        }
        return new MappingResult(Collections.<String>emptyList(), false, true, defaultValueConfigured(project, bean));
    }

}
