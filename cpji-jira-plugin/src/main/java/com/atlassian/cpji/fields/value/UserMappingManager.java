package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;

import static com.google.common.collect.Collections2.filter;

/**
 * @since v2.0
 */
public class UserMappingManager {
    private final UserManager userManager;

    public UserMappingManager(final UserManager userManager) {
        this.userManager = userManager;
    }

    public UserBean createUserBean(final String userKey) {
        Assertions.notBlank("userKey", userKey);
        ApplicationUser user = userManager.getUserByKey(userKey);
        return user == null ? null :
                new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
    }

    public CachingUserMapper getUserMapper() {
        return new CachingUserMapper(filter(userManager.getUsers(), ApplicationUser::isActive));
    }
}
