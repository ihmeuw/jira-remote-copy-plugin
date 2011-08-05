package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.config.CopyIssueConfigurationManager;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.util.UserManager;
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
    private CopyIssueConfigurationManager copyIssueConfigurationManager;
    private static final Logger log = Logger.getLogger(UserMappingManager.class);

    public UserMappingManager(final UserManager userManager, final CopyIssueConfigurationManager copyIssueConfigurationManager)
    {
        this.userManager = userManager;
        this.copyIssueConfigurationManager = copyIssueConfigurationManager;
    }

    public User mapUser(UserBean userBean)
    {
        switch (copyIssueConfigurationManager.getUserMappingType())
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
                userManager.getUserObject(userBean.getUserName());
        }
        return null;
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
                log.debug("Mapped remote user with username: '" + userBean.getUserName() + "' and email: '" + userBean.getEmail() + "' to local user with username: '" + user.getName() + "'");
                return user;
            }
        }
        log.warn("Could not find a local user for remote user with username: '" + userBean.getUserName() + "' and email: '" + userBean.getEmail() + "' returning no user");
        return null;
    }

    public List<User> getUsersByEmail(String email)
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
