package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.ReporterSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.Lists;

import java.util.Collections;

/**
 *
 * @since v1.4
 */
public class ReporterFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final PermissionManager permissionManager;
    private final UserMappingManager userMappingManager;

    public ReporterFieldMapper(final PermissionManager permissionManager, final Field field, final UserMappingManager userMappingManager)
    {
        super(field);
        this.permissionManager = permissionManager;
        this.userMappingManager = userMappingManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return ReporterSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        final User reporter = findUser(bean.getReporter(), project);
        if (reporter != null)
        {
            inputParameters.setReporterId(reporter.getName());
        }
        else if(fieldLayoutItem.isRequired())
        {
            inputParameters.setReporterId(project.getLeadUserName());
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
           return new MappingResult(Collections.<String>emptyList(), true, true);
        }
        final User reporter = findUser(bean.getReporter(), project);
        if (reporter == null)
        {
            return new MappingResult(Lists.newArrayList(bean.getReporter().getUserName()), false, false);
        }
        return new MappingResult(Collections.<String>emptyList(), true, false);
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
