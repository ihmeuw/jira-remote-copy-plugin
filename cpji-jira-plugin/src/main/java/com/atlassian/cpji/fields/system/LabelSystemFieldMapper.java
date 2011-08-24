package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;
import java.util.List;

/**
 * @since v1.4
 */
public class LabelSystemFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    public LabelSystemFieldMapper(final Field field)
    {
        super(field);
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<String> labels = bean.getLabels();
        if (labels != null && !labels.isEmpty())
        {
            return new MappingResult(Collections.<String>emptyList(), true, false);
        }
        return new MappingResult(Collections.<String>emptyList(), false, true);
    }

    public Class<? extends OrderableField> getField()
    {
        return LabelsSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        List<String> labels = bean.getLabels();
        if (labels != null && !labels.isEmpty())
        {
            String[] labelArray = new String[labels.size()];
            labels.toArray(labelArray);
            inputParameters.getActionParameters().put(IssueFieldConstants.LABELS, labelArray);
        }
    }
}
