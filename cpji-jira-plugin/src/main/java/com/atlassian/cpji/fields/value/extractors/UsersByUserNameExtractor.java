package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public class UsersByUserNameExtractor extends CachedExtractor {
    public UsersByUserNameExtractor(UserSearchService userSearchService) {
        super(userSearchService);
    }

    @Nonnull
    @Override
    protected Collection<ApplicationUser> fetchUsersDirectly(@Nonnull String phrase) {
        ApplicationUser userByName = userSearchService.getUserByName(null, phrase);
        return userByName == null ? Collections.emptyList() : Lists.newArrayList(userByName);
    }
}
