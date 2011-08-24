package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.util.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since v2.0
 */
public class VoterFieldMapper extends AbstractFieldMapper implements SystemFieldPostIssueCreationFieldMapper, NonOrderableSystemFieldMapper
{
    private final VoteService voteService;
    private PermissionManager permissionManager;
    private UserManager userManager;
    private JiraAuthenticationContext jiraAuthenticationContext;

    public VoterFieldMapper(final Field field, final VoteService voteService, final PermissionManager permissionManager, final UserManager userManager, final JiraAuthenticationContext jiraAuthenticationContext)
    {
        super(field);
        this.voteService = voteService;
        this.permissionManager = permissionManager;
        this.userManager = userManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VOTERS_AND_WATCHERS, project, user) && voteService.isVotingEnabled();
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        final List<String> voters = bean.getVoters();
        if (voters == null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, true);
        }
        if (!voteService.isVotingEnabled())
        {
            return new MappingResult(voters, false, false);
        }
        final List<String> unmappedUsers = new ArrayList<String>();
        final List<String> mappedUsers = new ArrayList<String>();
        for (String voter : voters)
        {
            User user = userManager.getUserObject(voter);
            if (user == null)
            {
                unmappedUsers.add(voter);
            }
            else
            {
                if (!permissionManager.hasPermission(Permissions.BROWSE, project, user))
                {
                    unmappedUsers.add(voter);
                }
                else
                {
                    mappedUsers.add(voter);
                }
            }
        }
        if (unmappedUsers.isEmpty())
        {
            return new MappingResult(unmappedUsers, true, false);
        }
        return new MappingResult(unmappedUsers, !mappedUsers.isEmpty(), false);
    }

    public void process(final Issue issue, final CopyIssueBean bean) throws FieldCreationException
    {
        if (permissionManager.hasPermission(Permissions.VIEW_VOTERS_AND_WATCHERS, issue.getProjectObject(), jiraAuthenticationContext.getLoggedInUser()) && voteService.isVotingEnabled())
        {
            final List<String> errors = new ArrayList<String>();
            final List<String> voters = bean.getVoters();
            if (voters != null)
            {
                for (String voter : voters)
                {
                    User user = userManager.getUserObject(voter);
                    if (user != null)
                    {
                        VoteService.VoteValidationResult voteValidationResult = voteService.validateAddVote(jiraAuthenticationContext.getLoggedInUser(), user, issue);
                        if (voteValidationResult.isValid())
                        {
                            voteService.addVote(jiraAuthenticationContext.getLoggedInUser(), voteValidationResult);
                        }
                        else
                        {
                            errors.add("Error when adding voter '" + user.getName() + "' ");
                            errors.addAll(voteValidationResult.getErrorCollection().getErrorMessages());
                        }
                    }
                }
                if (!errors.isEmpty())
                {
                    throw new FieldCreationException("Error(s) adding voters " + errors.toString(), getFieldId());
                }
            }
        }
    }

    public boolean isVisible()
    {
        return voteService.isVotingEnabled();
    }
}
