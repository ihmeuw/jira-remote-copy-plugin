package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;

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
        User user = userManager.getUserObject(userName);
        return new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
    }

	public CachingUserMapper getUserMapper() {
		return new CachingUserMapper(userManager.getUsers());
	}

}
