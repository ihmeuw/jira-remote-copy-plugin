package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
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
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.Lists;

import java.util.Collections;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 *
 * @since v1.4
 */
public class ReporterFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final PermissionManager permissionManager;
    private final UserMappingManager userMappingManager;
	private final JiraAuthenticationContext authenticationContext;

	public ReporterFieldMapper(final PermissionManager permissionManager, final FieldManager fieldManager, 
			final UserMappingManager userMappingManager, final DefaultFieldValuesManager defaultFieldValuesManager,
			final JiraAuthenticationContext authenticationContext)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.REPORTER), defaultFieldValuesManager);
        this.permissionManager = permissionManager;
        this.userMappingManager = userMappingManager;
		this.authenticationContext = authenticationContext;
	}

    public Class<? extends OrderableField> getField()
    {
        return ReporterSystemField.class;
    }

	@Override
	public void populateInputParams(IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {

		final User loggedIn = authenticationContext.getLoggedInUser();
		final User reporter = copyIssueBean.getReporter() != null ? findUser(copyIssueBean.getReporter(), project) : null;

		if (!fieldLayoutItem.isHidden()) {
			if (reporter != null) {
				inputParameters.setReporterId(reporter.getName());
			} else if (fieldLayoutItem.isRequired()) {
				String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), getFieldId(), issueType.getName());
				if (defaultFieldValue != null && defaultFieldValue.length > 0) {
					inputParameters.setReporterId(defaultFieldValue[0]);
				} else if (loggedIn != null) {
					inputParameters.setReporterId(loggedIn.getName());
				}
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
