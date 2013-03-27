package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;

/**
 * @since v2.0
 */
public class UserMappingManager
{
    private final UserManager userManager;
    private static final Logger log = Logger.getLogger(UserMappingManager.class);

    public UserMappingManager(final UserManager userManager)
    {
        this.userManager = userManager;
    }

    public User mapUser(UserBean userBean)
    {
		if (userBean == null) {
			return null;
		}

		Collection<User> usersInScope = userManager.getUsers();
		List<User> matchedUsers = getUsersByEmail(userBean, usersInScope);
		if (matchedUsers.size() == 1) {
			log.debug(String.format("Mapped remote user by email: '%s' and email: '%s' to local user with user name: '%s'", userBean.getUserName(),  userBean.getEmail(), matchedUsers.get(0).getName()));
			return matchedUsers.get(0);
		}
        if (!matchedUsers.isEmpty())
		{
			usersInScope = matchedUsers;
		}

		// now limit users by full name
		matchedUsers = getUsersByFullName(userBean, usersInScope);
		if (matchedUsers.size() == 1) {
			log.debug(String.format("Mapped remote user by full name: '%s' and email: '%s' to local user with user name: '%s'", userBean.getUserName(),  userBean.getEmail(), matchedUsers.get(0).getName()));
			return matchedUsers.get(0);
		}

		// finally try username
		matchedUsers = getUsersByUserName(userBean, usersInScope);
		if (matchedUsers.size() == 1) {
			log.debug(String.format("Mapped remote user by user name: '%s' and email: '%s' to local user with user name: '%s'", userBean.getUserName(),  userBean.getEmail(), matchedUsers.get(0).getName()));
			return matchedUsers.get(0);
		}

		log.warn(String.format("Could not find a local user for remote user with user name: '%s' and email: '%s' returning no user", userBean.getUserName(), userBean.getEmail()));
		return null;
    }

    public UserBean createUserBean(final String userName)
    {
        Assertions.notBlank("userName", userName);
        User user = userManager.getUserObject(userName);
        return new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
    }

    private List<User> getUsersByEmail(UserBean userBean, Collection<User> usersInScope)
    {
        final List<User> users = Lists.newArrayListWithCapacity(3);
        final String emailAddress = StringUtils.trimToNull(userBean.getEmail());
        if (emailAddress != null)
        {
            for (User user : usersInScope)
            {
                if (emailAddress.equalsIgnoreCase(user.getEmailAddress()))
                {
                    users.add(user);
                }
            }
        }
        return users;
    }

	private List<User> getUsersByFullName(UserBean userBean, Collection<User> usersInScope)
	{
		final List<User> users = Lists.newArrayListWithCapacity(3);
		final String fullName = StringUtils.trimToNull(userBean.getFullName());
		if (fullName != null)
		{
			for (User user : usersInScope)
			{
				if (fullName.equalsIgnoreCase(user.getDisplayName()))
				{
					users.add(user);
				}
			}
		}
		return users;
	}

	private List<User> getUsersByUserName(UserBean userBean, Collection<User> usersInScope)
	{
		final List<User> users = Lists.newArrayListWithCapacity(3);
		final String userName = StringUtils.trimToNull(userBean.getUserName());
		if (userName != null)
		{
			for (User user : usersInScope)
			{
				if (userName.equalsIgnoreCase(user.getName()))
				{
					users.add(user);
				}
			}
		}
		return users;
	}

}
