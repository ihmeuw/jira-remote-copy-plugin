package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.config.UserMappingType;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
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

    public User mapUser(UserBean userBean, final Project project)
    {
        UserMappingType userMappingType = UserMappingType.BY_EMAIL_AND_USERNAME;
        switch (userMappingType)
        {
            case BY_E_MAIL:
                return byEmail(userBean);
            case BY_EMAIL_AND_USERNAME:
                User user = byEmail(userBean);
                if (user != null && user.getName().equals(userBean.getUserName()))
                {
                    return user;
                }
                return null;
            case BY_USERNAME:
                return userManager.getUserObject(userBean.getUserName());
            default:
                log.warn("No valid user mapping type '" + userMappingType + "' mapping user '" + userBean.getUserName() + "' by username.");
                return userManager.getUserObject(userBean.getUserName());
        }
    }

    public UserBean createUserBean(final String userName)
    {
        Assertions.notBlank("userName", userName);
        User user = userManager.getUserObject(userName);
        return new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
    }

    private User byEmail(final UserBean userBean)
    {
        List<User> usersByEmail = getUsersByEmail(userBean.getEmail());
        if (usersByEmail.isEmpty())
        {
            return null;
        }
        else if (usersByEmail.size() == 1)
        {
            return usersByEmail.get(0);
        }

        for (User user : usersByEmail)
        {
            if (user.getName().equalsIgnoreCase(userBean.getUserName()))
            {
                log.debug("Mapped remote user with user name: '" + userBean.getUserName() + "' and email: '" + userBean.getEmail() + "' to local user with user name: '" + user.getName() + "'");
                return user;
            }
        }
        log.warn("Could not find a local user for remote user with user name: '" + userBean.getUserName() + "' and email: '" + userBean.getEmail() + "' returning no user");
        return null;
    }

    private List<User> getUsersByEmail(String email)
    {
        List<User> users = new ArrayList();
        String emailAddress = StringUtils.trimToNull(email);
        if (emailAddress != null)
        {
            for (User user : userManager.getUsers())
            {
                if (emailAddress.equalsIgnoreCase(user.getEmailAddress()))
                {
                    users.add(user);
                }
            }
        }
        return users;
    }

}
