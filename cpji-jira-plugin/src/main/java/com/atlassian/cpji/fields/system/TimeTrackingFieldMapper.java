package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.TimeTrackingBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.TimeTrackingSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.JiraDurationUtils;

import java.util.Collections;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v2.1
 */
public class TimeTrackingFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final TimeTrackingConfiguration timeTrackingConfiguration;

    public TimeTrackingFieldMapper(FieldManager fieldManager, final TimeTrackingConfiguration timeTrackingConfiguration, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.TIMETRACKING), defaultFieldValuesManager);
        this.timeTrackingConfiguration = timeTrackingConfiguration;
    }

    @Override
    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    @Override
    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        TimeTrackingBean timeTracking = bean.getTimeTracking();
        if (!timeTrackingConfiguration.enabled())
        {
            return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
        }
        return new MappingResult(Collections.<String>emptyList(), timeTracking != null, timeTracking == null, hasDefaultValue(
				project, bean));
    }

    @Override
    public Class<? extends OrderableField> getField()
    {
        return TimeTrackingSystemField.class;
    }

    @Override
    public void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {

        TimeTrackingBean timeTracking = bean.getTimeTracking();
        if (timeTrackingConfiguration.enabled() && timeTracking != null)
        {
            final Long originalEstimate = timeTracking.getOriginalEstimate();
            if(originalEstimate != null){
                final JiraDurationUtils durutil = ComponentAccessor.getComponent(JiraDurationUtils.class);
                final String formattedValue = durutil.getShortFormattedDuration(originalEstimate);
                if (timeTrackingConfiguration.getMode().equals(TimeTrackingConfiguration.Mode.LEGACY))
                {
                    inputParameters.getActionParameters().put(IssueFieldConstants.TIMETRACKING, toArr(formattedValue));
                }
                else
                {
                    inputParameters.getActionParameters().put(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE, toArr(formattedValue));
                    inputParameters.getActionParameters().put(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE, toArr(formattedValue));
                }
            }
        }
    }

    private String[] toArr(final String fieldValue)
    {
        return new String[] { fieldValue };
    }
}
