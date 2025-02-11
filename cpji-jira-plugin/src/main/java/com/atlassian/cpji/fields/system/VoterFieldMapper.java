package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since v2.0
 */
public class VoterFieldMapper extends AbstractFieldMapper
        implements PostIssueCreationFieldMapper, NonOrderableSystemFieldMapper
{
    private final VoteService voteService;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public VoterFieldMapper(final Field field, final VoteService voteService, final PermissionManager permissionManager,
			final JiraAuthenticationContext jiraAuthenticationContext, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(field, defaultFieldValuesManager);
        this.voteService = voteService;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public boolean userHasRequiredPermission(final Project project, final ApplicationUser user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VOTERS_AND_WATCHERS, project, user) && voteService.isVotingEnabled();
    }

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        final List<UserBean> voters = bean.getVoters();
        if (voters == null)
        {
            return new MappingResult(Collections.<String>emptyList(), true, true, hasDefaultValue(project, bean));
        }
        if (!voteService.isVotingEnabled())
        {
            ArrayList<String> unMappedUsers = Lists.newArrayList(Iterables.transform(voters, new Function<UserBean, String>()
            {
                public String apply(final UserBean from)
                {
                    return from.getUserName();
                }
            }));
            return new MappingResult(unMappedUsers, false, false, hasDefaultValue(project, bean));
        }
        final List<String> unmappedUsers = new ArrayList<String>();
        final List<String> mappedUsers = new ArrayList<String>();
        for (UserBean voter : voters)
        {
            ApplicationUser user = userMapper.mapUser(voter);
            if (user == null)
            {
                unmappedUsers.add(voter.getUserName());
            }
            else
            {
                if (!permissionManager.hasPermission(Permissions.BROWSE, project, user))
                {
                    unmappedUsers.add(voter.getUserName());
                }
                else
                {
                    mappedUsers.add(voter.getUserName());
                }
            }
        }
        if (unmappedUsers.isEmpty())
        {
            return new MappingResult(unmappedUsers, true, false, hasDefaultValue(project, bean));
        }
        return new MappingResult(unmappedUsers, !mappedUsers.isEmpty(), false, hasDefaultValue(project, bean));
    }

    public void process(final CachingUserMapper userMapper, final Issue issue, final CopyIssueBean bean) throws FieldCreationException
    {
        if (permissionManager.hasPermission(Permissions.VIEW_VOTERS_AND_WATCHERS, issue.getProjectObject(),
				jiraAuthenticationContext.getLoggedInUser()) && voteService.isVotingEnabled())
        {
            final List<String> errors = new ArrayList<String>();
            final List<UserBean> voters = bean.getVoters();
			final ApplicationUser reporter = issue.getReporterUser();
            if (voters != null)
            {
                for (UserBean voter : voters)
                {
                    ApplicationUser user = userMapper.mapUser(voter);
                    if (user != null && (reporter == null || !user.equals(reporter)))
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
