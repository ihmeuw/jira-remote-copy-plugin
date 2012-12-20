package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @since v1.4
 */
public class AssigneeFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;
    private final UserMappingManager userMappingManager;

    private static class InternalMappingResult{
        private final User mappedUser;
        private final MappingResultDecision decision;
        public enum MappingResultDecision {
            FOUND,NOT_FOUND
        }

        private InternalMappingResult(User mappedUser, MappingResultDecision decision) {
            this.mappedUser = mappedUser;
            this.decision = decision;
        }
    }

    public AssigneeFieldMapper(PermissionManager permissionManager, final ApplicationProperties applicationProperties, Field field, final UserMappingManager userMappingManager)
    {
        super(field);
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
        this.userMappingManager = userMappingManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return AssigneeSystemField.class;
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.ASSIGN_ISSUE, project, user);
    }


    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
        InternalMappingResult mapResult = mapUser(bean.getAssignee(), project);
        switch(mapResult.decision){
            case FOUND:
                return new MappingResult(ImmutableList.<String>of(), true, false);
            case NOT_FOUND:
                List<String> unmapped = bean.getAssignee() != null?
                        ImmutableList.of(bean.getAssignee().getUserName()):
                        ImmutableList.<String>of();
                if(unassignedAllowed){
                    return new MappingResult(unmapped, true, unmapped.isEmpty());
                } else {
                    return new MappingResult(unmapped, false, unmapped.isEmpty());
                }
            default:
                return null;
        }
    }

    private InternalMappingResult mapUser(UserBean user, final Project project){
        if(user == null){
            return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
        }
        User assignee = findUser(user, project);
        if(assignee == null){
            return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
        } else {
            if (permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, assignee)){
                return new InternalMappingResult(assignee, InternalMappingResult.MappingResultDecision.FOUND);
            } else {
                return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
            }
        }



    }



    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        InternalMappingResult assignee = mapUser(bean.getAssignee(), project);
        boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
        switch(assignee.decision){
            case FOUND:
                inputParameters.setAssigneeId(assignee.mappedUser.getName());
                break;
            case NOT_FOUND:
                if(!unassignedAllowed){
                    inputParameters.setAssigneeId(project.getLeadUserName());
                }
                break;

        }
    }

    private User findUser(final UserBean user, final Project project)
    {
        return userMappingManager.mapUser(user, project);
    }
}
