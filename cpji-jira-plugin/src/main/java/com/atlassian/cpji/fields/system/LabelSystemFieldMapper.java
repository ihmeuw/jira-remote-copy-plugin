package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;
import java.util.List;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class LabelSystemFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    public LabelSystemFieldMapper(final FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.LABELS), defaultFieldValuesManager);
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
            return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
        }
        return new MappingResult(Collections.<String>emptyList(), false, true, hasDefaultValue(project, bean));
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
