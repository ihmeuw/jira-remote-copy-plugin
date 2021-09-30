package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.cpji.fields.value.UsersMap;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Abstract layer for extracting users from cache (if present) or directly via service.
 */
public abstract class CachedExtractor {

    protected UserSearchService userSearchService;
    protected UsersMap usersMap;

    public CachedExtractor(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
        usersMap = new UsersMap();
    }

    /**
     * Extracts active users from cache if present. Otherwise, fetches directly via service.
     *
     * @param phrase exact key used to fetch users
     * @return collection of active ApplicationUser
     */
    @Nonnull
    public Collection<ApplicationUser> getBy(@Nullable String phrase) {
        if (StringUtils.isBlank(phrase)) {
            return Collections.emptyList();
        }
        Collection<ApplicationUser> cachedUsers = usersMap.get(phrase);
        if (cachedUsers == null) {
            Collection<ApplicationUser> users = filterActive(fetchUsersDirectly(phrase));
            usersMap.put(phrase, users);
            return users;
        }
        return cachedUsers;
    }

    @Nonnull
    protected abstract Collection<ApplicationUser> fetchUsersDirectly(@Nonnull String phrase);

    @Nonnull
    protected Collection<ApplicationUser> filterActive(@Nonnull Collection<ApplicationUser> users) {
        return users
                .stream()
                .filter(ApplicationUser::isActive)
                .collect(Collectors.toList());
    }
}
