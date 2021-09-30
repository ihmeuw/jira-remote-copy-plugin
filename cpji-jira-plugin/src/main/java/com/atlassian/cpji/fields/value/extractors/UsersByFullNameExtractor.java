package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.Collection;

public class UsersByFullNameExtractor extends CachedExtractor {

    public UsersByFullNameExtractor(UserSearchService userSearchService) {
        super(userSearchService);
    }

    @Nonnull
    @Override
    protected Collection<ApplicationUser> fetchUsersDirectly(@Nonnull String phrase) {
        return Lists.newArrayList(userSearchService.findUsersByFullName(phrase));
    }
}
