package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;
import com.google.common.base.Predicate;

import static com.google.common.collect.Collections2.filter;

/**
 * @since v2.0
 */
public class UserMappingManager
{
    private final UserManager userManager;

	public UserMappingManager(final UserManager userManager)
    {
        this.userManager = userManager;
    }

    public UserBean createUserBean(final String userName)
    {
        Assertions.notBlank("userName", userName);
        ApplicationUser user = userManager.getUserObject(userName);
        if (user == null)
        {
            return null;
        }
        else
        {
            return new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
        }
    }

    public CachingUserMapper getUserMapper() {
        return new CachingUserMapper(filter(userManager.getUsers(), ApplicationUser::isActive));
    }
}
