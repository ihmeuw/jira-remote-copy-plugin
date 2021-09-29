package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

import java.util.Collection;

public class UsersByEmailExtractor extends CachedExtractor {

    public UsersByEmailExtractor(UserSearchService userSearchService) {
        super(userSearchService);
    }

    @Override
    protected Collection<ApplicationUser> fetchUsersDirectly(String phrase) {
        return Lists.newArrayList(userSearchService.findUsersByEmail(phrase));
    }
}
