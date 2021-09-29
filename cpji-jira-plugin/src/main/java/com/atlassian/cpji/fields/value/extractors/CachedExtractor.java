package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.cpji.fields.value.UsersMap;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/**
 * Abstract layer for extracting Users from cache (if present) or directly via service.
 */
public abstract class CachedExtractor {

    protected UserSearchService userSearchService;
    protected UsersMap usersMap;

    public CachedExtractor(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
        usersMap = new UsersMap();
    }

    /**
     * Extracts users from cache if present. Otherwise, fetches directly via service.
     * @param phrase key to fetch users
     * @return collection of ApplicationUser
     */
    @Nonnull
    public Collection<ApplicationUser> getBy(String phrase) {
        if (StringUtils.isBlank(phrase)) {
            return Collections.emptyList();
        }
        Collection<ApplicationUser> cachedUsers = usersMap.get(phrase);
        if (cachedUsers == null) {
            Collection<ApplicationUser> users = fetchUsersDirectly(phrase);
            usersMap.put(phrase, users);
            return users;
        }
        return cachedUsers;
    }

    protected abstract Collection<ApplicationUser> fetchUsersDirectly(String phrase);
}
