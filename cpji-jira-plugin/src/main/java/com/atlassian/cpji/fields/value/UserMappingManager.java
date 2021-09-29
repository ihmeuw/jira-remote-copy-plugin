package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.dbc.Assertions;

/**
 * @since v2.0
 */
public class UserMappingManager {
    private final UserManager userManager;
    private final UserSearchService userSearchService;

    public UserMappingManager(final UserManager userManager,
                              final UserSearchService userSearchService) {
        this.userManager = userManager;
        this.userSearchService = userSearchService;
    }

    public UserBean createUserBean(final String userKey) {
        Assertions.notBlank("userKey", userKey);
        ApplicationUser user = userManager.getUserByKey(userKey);
        return user == null ? null :
                new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName());
    }

    public CachingUserMapper getUserMapper() {
        return new CachingUserMapper(userSearchService);
    }
}
