package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;

public class UsersByUserNameExtractor extends CachedExtractor {
    public UsersByUserNameExtractor(UserSearchService userSearchService) {
        super(userSearchService);
    }

    @Override
    protected Collection<ApplicationUser> fetchUsersDirectly(String phrase) {
        //TODO: LOTUS-501: use multiple of users
        ApplicationUser userByName = userSearchService.getUserByName(null, phrase);
        return userByName == null ? Collections.emptyList() : Lists.newArrayList(userByName);
    }
}
