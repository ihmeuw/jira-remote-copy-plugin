package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CommentBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.util.SimpleErrorCollection;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @since v1.4
 */
public class CommentFieldMapper extends AbstractFieldMapper implements SystemFieldPostIssueCreationFieldMapper
{
    private final CommentService commentService;
    private final ProjectRoleManager projectRoleManager;
    private final GroupManager groupManager;
    private final PermissionManager permissionsManager;
    private final UserMappingManager userMappingManager;

    public CommentFieldMapper(CommentService commentService, ProjectRoleManager projectRoleManager, GroupManager groupManager, final PermissionManager permissionManager, final OrderableField field, final UserMappingManager userMappingManager)
    {
        super(field);
        this.commentService = commentService;
        this.projectRoleManager = projectRoleManager;
        this.groupManager = groupManager;
        this.permissionsManager = permissionManager;
        this.userMappingManager = userMappingManager;
    }

    public void process(final Issue issue, final CopyIssueBean bean)
    {
        List<CommentBean> comments = bean.getComments();
        if (comments != null)
        {
            for (CommentBean comment : comments)
            {
                ResultHolder<User> userResultHolder = findUser(comment.getAuthor(), issue.getProjectObject());
                ResultHolder<Group> groupResultHolder = findGroup(comment.getGroupLevel());
                ResultHolder<ProjectRole> projectRole = findProjectRole(comment.getRoleLevel());
                if (userResultHolder.mapped && groupResultHolder.mapped && projectRole.mapped)
                {
                    commentService.create(userResultHolder.result, issue, comment.getBody(), (groupResultHolder.result != null) ? groupResultHolder.result.getName() : null, (projectRole.result != null) ? projectRole.result.getId() : null, comment.getCreated(), false, new SimpleErrorCollection());
                }
            }
        }
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionsManager.hasPermission(Permissions.COMMENT_ISSUE, project, user);
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedFieldValues = new ArrayList<String>();
        List<CommentBean> comments = bean.getComments();
        if (comments != null)
        {
            for (CommentBean comment : comments)
            {
                ResultHolder<User> userResultHolder = findUser(comment.getAuthor(), project);
                if (!userResultHolder.mapped)
                {
                   unmappedFieldValues.add("User " + comment.getAuthor() + " can't comment on this issue.");
                }
                ResultHolder<Group> groupResultHolder = findGroup(comment.getGroupLevel());
                if (!groupResultHolder.mapped)
                {
                    unmappedFieldValues.add("Group " + comment.getGroupLevel() + "not found.");
                }
                ResultHolder<ProjectRole> projectRoleResultHolder = findProjectRole(comment.getRoleLevel());
                if (!projectRoleResultHolder.mapped)
                {
                    unmappedFieldValues.add("Project Role " + comment.getRoleLevel() + "not found.");
                }
            }
            return new MappingResult(unmappedFieldValues, unmappedFieldValues.isEmpty(), false);
        }
        return new MappingResult(unmappedFieldValues, unmappedFieldValues.isEmpty(), true);
    }

    private class ResultHolder<T>
    {
        public T result;
        public boolean mapped;

        private ResultHolder(final T result, final boolean mapped)
        {
            this.result = result;
            this.mapped = mapped;
        }

        private ResultHolder(final boolean mapped)
        {
            this.mapped = mapped;
        }
    }

    private ResultHolder<ProjectRole> findProjectRole(final String roleLevel)
    {
        if (StringUtils.isNotEmpty(roleLevel))
        {
            ProjectRole projectRole = projectRoleManager.getProjectRole(roleLevel);
            if (projectRole == null)
            {
                return new ResultHolder<ProjectRole>(false);
            }
            return new ResultHolder<ProjectRole>(projectRole, true);
        }
        return new ResultHolder<ProjectRole>(true);
    }

    private ResultHolder<Group> findGroup(final String groupLevel)
    {
        if (StringUtils.isNotEmpty(groupLevel))
        {
            Group groupObject = groupManager.getGroupObject(groupLevel);
            if (groupObject == null)
            {
                return new ResultHolder<Group>(false);
            }
            return new ResultHolder<Group>(groupObject, true);
        }
        return new ResultHolder<Group>(true);
    }

    private ResultHolder<User> findUser(final UserBean userBean, final Project project)
    {
        User user = userMappingManager.mapUser(userBean, project);
        if (user == null)
        {
            return new ResultHolder<User>(false);
        }
        if (permissionsManager.hasPermission(Permissions.COMMENT_ISSUE, project, user))
        {
            return new ResultHolder<User>(user, true);
        }
        return new ResultHolder<User>(false);
    }
}
