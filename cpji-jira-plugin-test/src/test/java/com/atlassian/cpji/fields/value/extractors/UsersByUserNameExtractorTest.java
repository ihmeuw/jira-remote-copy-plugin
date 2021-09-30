package com.atlassian.cpji.fields.value.extractors;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsersByUserNameExtractorTest {

    @Mock
    private UserSearchService userSearchService;

    @Mock
    private ApplicationUser user;

    private CachedExtractor extractor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        extractor = new UsersByUserNameExtractor(userSearchService);
    }

    @Test
    void testGetByWhenUserNameIsPresent() {
        when(user.isActive()).thenReturn(true);
        String userName = "Full Name";
        when(userSearchService.getUserByName(null, userName)).thenReturn(user);

        Collection<ApplicationUser> result = extractor.getBy(userName);
        assertThat(result, contains(user));
    }

    @Test
    void testGetByWhenUserNameIsPresentButInactive() {
        when(user.isActive()).thenReturn(false);
        String userName = "Full Name";
        when(userSearchService.getUserByName(null, userName)).thenReturn(user);

        Collection<ApplicationUser> result = extractor.getBy(userName);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenUserNameIsNull() {
        Collection<ApplicationUser> result = extractor.getBy(null);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenUserNameIsBlank() {
        Collection<ApplicationUser> result = extractor.getBy("");
        assertThat(result, empty());
    }

    @Test
    void testGetByFetchedFromCacheAfterFirstTimeWhenUserNameIsPresent() {
        when(user.isActive()).thenReturn(true);
        String userName = "Full Name";
        when(userSearchService.getUserByName(null, userName)).thenReturn(user);

        extractor.getBy(userName);
        extractor.getBy(userName);
        Collection<ApplicationUser> result = extractor.getBy(userName);

        verify(userSearchService).getUserByName(null, userName);
        assertThat(result, contains(user));
    }
}