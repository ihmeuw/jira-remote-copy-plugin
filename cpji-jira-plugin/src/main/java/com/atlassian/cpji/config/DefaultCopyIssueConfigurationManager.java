package com.atlassian.cpji.config;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.issue.util.VisibilityValidator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.util.dbc.Assertions;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since v2.0
 */
public class DefaultCopyIssueConfigurationManager implements CopyIssueConfigurationManager
{
    private PluginSettingsFactory pluginSettingsFactory;
    private GroupManager groupManager;
    private JiraAuthenticationContext jiraAuthenticationContext;
    private VisibilityValidator visibilityValidator;
    private ProjectRoleManager projectRoleManager;
    public static final String TYPE = ".type";

    private static final Logger log = Logger.getLogger(DefaultCopyIssueConfigurationManager.class);

    public static final String CPJI = "cpji.";
    public static final String CPJI_SECURITY_LEVEL_KEY = CPJI + "security.level.%s";
    private static final String CPJI_USER_MAPPING_KEY = CPJI + "user.mapping.%s";

    public DefaultCopyIssueConfigurationManager
            (
                    final PluginSettingsFactory pluginSettingsFactory,
                    final JiraAuthenticationContext jiraAuthenticationContext,
                    final GroupManager groupManager,
                    final VisibilityValidator visibilityValidator,
                    final ProjectRoleManager projectRoleManager)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.groupManager = groupManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.visibilityValidator = visibilityValidator;
        this.projectRoleManager = projectRoleManager;
    }

    private CommentSecurityLevel findCommentSecurityLevel(final CommentSecurityLevel commentSecurityLevel, Project project)
    {
        if (commentSecurityLevel.isGroupLevel() && visibilityValidator.isGroupVisiblityEnabled())
        {
            Collection<Group> groupsForUser = groupManager.getGroupsForUser(jiraAuthenticationContext.getLoggedInUser().getName());
            try
            {
                Group group = Iterables.find(groupsForUser, new Predicate<Group>()
                {
                    public boolean apply(final Group input)
                    {
                        return commentSecurityLevel.getId().equals(input.getName());
                    }
                });
                return new CommentSecurityLevel(group.getName(), group.getName(), CommentSecurityType.GROUP);
            }
            catch (NoSuchElementException ex)
            {
                return null;
            }
        }
        else
        {
            Collection<ProjectRole> projectRoles = projectRoleManager.getProjectRoles(jiraAuthenticationContext.getLoggedInUser(), project);
            try
            {
                ProjectRole projectRole = Iterables.find(projectRoles, new Predicate<ProjectRole>()
                {
                    public boolean apply(final ProjectRole input)
                    {
                        String roleLevelId = commentSecurityLevel.getId();
                        return Long.valueOf(roleLevelId).equals(input.getId());
                    }
                });
                return new CommentSecurityLevel(String.valueOf(projectRole.getId()), projectRole.getName(), CommentSecurityType.ROLE);
            }
            catch (NoSuchElementException ex)
            {
                return null;
            }
        }
    }

    private String createKeyForSecurityLevel(final String projectKey)
    {
        return String.format(CPJI_SECURITY_LEVEL_KEY, projectKey);
    }

    private String createKeyForUserMapping(final String projectKey)
    {
        return String.format(CPJI_USER_MAPPING_KEY, projectKey);
    }

    public List<CommentSecurityLevel> getSecurityLevels(Project project)
    {
        final List<CommentSecurityLevel> commentSecurityLevels = new ArrayList<CommentSecurityLevel>();
        if (visibilityValidator.isGroupVisiblityEnabled())
        {
            Collection<Group> groupsForUser = groupManager.getGroupsForUser(jiraAuthenticationContext.getLoggedInUser().getName());
            if (!groupsForUser.isEmpty())
            {
                Iterable<CommentSecurityLevel> trans = Iterables.transform(groupsForUser, new Function<Group, CommentSecurityLevel>()
                {
                    public CommentSecurityLevel apply(final Group from)
                    {
                        String label = jiraAuthenticationContext.getI18nHelper().getText(CommentSecurityType.GROUP.getLabelKey(), from.getName());
                        return new CommentSecurityLevel(from.getName(), label, CommentSecurityType.GROUP);
                    }
                });
                commentSecurityLevels.addAll(Lists.newArrayList(trans));
            }
        }
        Collection<ProjectRole> projectRoles = projectRoleManager.getProjectRoles(jiraAuthenticationContext.getLoggedInUser(), project);
        if (!projectRoles.isEmpty())
        {
            Iterable<CommentSecurityLevel> transRoles = Iterables.transform(projectRoles, new Function<ProjectRole, CommentSecurityLevel>()
            {
                public CommentSecurityLevel apply(final ProjectRole from)
                {
                    String label = jiraAuthenticationContext.getI18nHelper().getText(CommentSecurityType.ROLE.getLabelKey(), from.getName());
                    return new CommentSecurityLevel(String.valueOf(from.getId()), label, CommentSecurityType.ROLE);
                }
            });
            commentSecurityLevels.addAll(Lists.newArrayList(transRoles));
        }
        return commentSecurityLevels;
    }

    public CommentSecurityLevel getCommentSecurityLevel(final Project project)
    {
        Assertions.notNull("project", project);
        final PluginSettings settings = pluginSettingsFactory.createSettingsForKey(project.getKey());
        String commentSecurityLevelId = (String) settings.get(createKeyForSecurityLevel(project.getKey()));
        String commentSecurityLevelType = (String) settings.get(createKeyForSecurityLevel(project.getKey()) + TYPE);
        if (commentSecurityLevelId == null || commentSecurityLevelType == null)
        {
            return null;
        }
        else
        {
            CommentSecurityType commentSecurityType = CommentSecurityType.valueOf(commentSecurityLevelType);
            if (commentSecurityType == null)
            {
                throw new IllegalStateException("Invalid comment security type '" + commentSecurityLevelType + "' specified.");
            }
            return findCommentSecurityLevel(new CommentSecurityLevel(commentSecurityLevelId, "", commentSecurityType), project);
        }
    }

    public UserMappingType getUserMappingType(final Project project)
    {
        PluginSettings settingsForKey = pluginSettingsFactory.createSettingsForKey(project.getKey());
        Object userMappingType = settingsForKey.get(createKeyForUserMapping(project.getKey()));
        if (userMappingType == null)
        {
            return UserMappingType.BY_USERNAME;
        }
        else
        {
            try
            {
                return UserMappingType.valueOf((String) userMappingType);
            }
            catch (Exception ex)
            {
                log.error("Failed to read user mapping type. Value '" + userMappingType + "' not a valid user mapping type. Using default '" + UserMappingType.BY_USERNAME + "'", ex);
                return UserMappingType.BY_USERNAME;
            }
        }
    }

    public void setUserMapping(final UserMappingType userMapping, final Project project)
    {
        PluginSettings settingsForKey = pluginSettingsFactory.createSettingsForKey(project.getKey());
        settingsForKey.put(createKeyForUserMapping(project.getKey()), userMapping.name());
    }


}


