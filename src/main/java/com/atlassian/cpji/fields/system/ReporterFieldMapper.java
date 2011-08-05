package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
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
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;

/**
 *
 * @since v1.4
 */
public class ReporterFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final UserManager userManager;
    private final PermissionManager permissionManager;

    public ReporterFieldMapper(UserManager userManager, final PermissionManager permissionManager, final Field field)
    {
        super(field);
        this.userManager = userManager;
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return ReporterSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        final User reporter = findUser(bean.getReporter());
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
        if (StringUtils.isEmpty(bean.getReporter()))
        {
           return new MappingResult(Collections.<String>emptyList(), true, true);
        }
        final User reporter = findUser(bean.getReporter());
        if (reporter == null)
        {
            return new MappingResult(Lists.newArrayList(bean.getReporter()), false, false);
        }
        return new MappingResult(Collections.<String>emptyList(), true, false);
    }

    private User findUser(final String username)
    {
        return userManager.getUserObject(username);
    }

    public String getFieldId()
    {
        return IssueFieldConstants.REPORTER;
    }
}
