package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.ReporterSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 *
 * @since v1.4
 */
public class ReporterFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final PermissionManager permissionManager;
    private final UserMappingManager userMappingManager;

	public ReporterFieldMapper(final PermissionManager permissionManager, final FieldManager fieldManager, 
			final UserMappingManager userMappingManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.REPORTER), defaultFieldValuesManager);
        this.permissionManager = permissionManager;
        this.userMappingManager = userMappingManager;
	}

    public Class<? extends OrderableField> getField()
    {
        return ReporterSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        if (bean.getReporter() != null)
        {
            final User reporter = findUser(bean.getReporter(), project);
            if (reporter != null)
            {
                inputParameters.setReporterId(reporter.getName());
            }
        } else if (fieldLayoutItem.isRequired() && hasDefaultValue(project, bean)) {
			String[] defaults = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), fieldLayoutItem.getOrderableField().getId(), bean.getTargetIssueType());
			if (StringUtils.isNotBlank(defaults[0])) {
				inputParameters.setReporterId(defaults[0]);
			}
		}
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.MODIFY_REPORTER, project, user);
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        if (bean.getReporter() == null)
        {
           return new MappingResult(Collections.<String>emptyList(), true, true, true);
        }
        final User reporter = findUser(bean.getReporter(), project);
        if (reporter == null)
        {
            return new MappingResult(Lists.newArrayList(bean.getReporter().getUserName()), false, false, true);
        }
        return new MappingResult(Collections.<String>emptyList(), true, false, true);
    }

    private User findUser(final UserBean user, final Project project)
    {
        return userMappingManager.mapUser(user, project);
    }

    public String getFieldId()
    {
        return IssueFieldConstants.REPORTER;
    }
}
