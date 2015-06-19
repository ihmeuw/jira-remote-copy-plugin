package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueUtils;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class AssigneeFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;

    static class InternalMappingResult {
        final ApplicationUser mappedUser;
        final MappingResultDecision decision;

        public enum MappingResultDecision {
            FOUND, NOT_FOUND
        }

        InternalMappingResult(ApplicationUser mappedUser, MappingResultDecision decision) {
            this.mappedUser = mappedUser;
            this.decision = decision;
        }
    }

    public AssigneeFieldMapper(PermissionManager permissionManager, final ApplicationProperties applicationProperties,
                               FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager) {
        super(getOrderableField(fieldManager, IssueFieldConstants.ASSIGNEE), defaultFieldValuesManager);
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
    }

    public Class<? extends OrderableField> getField() {
        return AssigneeSystemField.class;
    }

    public boolean userHasRequiredPermission(final Project project, final ApplicationUser user) {
        return permissionManager.hasPermission(Permissions.ASSIGN_ISSUE, project, user);
    }


    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project) {
        boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
        InternalMappingResult mapResult = mapUser(userMapper, bean.getAssignee(), project);
        switch (mapResult.decision) {
            case FOUND:
                return new MappingResult(ImmutableList.<String>of(), true, false, true);
            case NOT_FOUND:
                List<String> unmapped = bean.getAssignee() != null ?
                        ImmutableList.of(bean.getAssignee().getUserName()) :
                        ImmutableList.<String>of();
                if (unassignedAllowed) {
                    return new MappingResult(unmapped, true, unmapped.isEmpty(), true);
                } else {
                    return new MappingResult(unmapped, false, unmapped.isEmpty(), true);
                }
            default:
                return null;
        }
    }

    InternalMappingResult mapUser(CachingUserMapper userMapper, UserBean user, final Project project) {
        ApplicationUser assignee = userMapper.mapUser(user);
        if (assignee == null) {
            return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
        } else {
            if (permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, assignee)) {
                return new InternalMappingResult(assignee, InternalMappingResult.MappingResultDecision.FOUND);
            } else {
                return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
            }
        }
    }

    @Override
    public void populateInputParams(CachingUserMapper userMapper, IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
                                    FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
        InternalMappingResult assignee = mapUser(userMapper, copyIssueBean.getAssignee(), project);
        switch (assignee.decision) {
            case FOUND:
                inputParameters.setAssigneeId(assignee.mappedUser.getName());
                break;
            case NOT_FOUND:
                boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
                if (!unassignedAllowed) {
                    if (hasDefaultValue(project, copyIssueBean)) {
                        String[] defaults = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(),
                                fieldLayoutItem.getOrderableField().getId(), copyIssueBean.getTargetIssueType());
                        if (StringUtils.isNotBlank(defaults[0])) {
                            inputParameters.setAssigneeId(defaults[0]);
                        }
                    } else {
                        inputParameters.setAssigneeId(IssueUtils.AUTOMATIC_ASSIGNEE);
                    }
                }
                break;
        }
    }
}
