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
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsersByEmailExtractorTest {

    @Mock
    private UserSearchService userSearchService;

    @Mock
    private ApplicationUser user;

    private CachedExtractor extractor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        extractor = new UsersByEmailExtractor(userSearchService);
    }

    @Test
    void testGetByWhenEmailIsPresent() {
        when(user.isActive()).thenReturn(true);
        String email = "email@localhost";
        when(userSearchService.findUsersByEmail(email)).thenReturn(Collections.singletonList(user));

        Collection<ApplicationUser> result = extractor.getBy(email);
        assertThat(result, contains(user));
    }

    @Test
    void testGetByWhenEmailIsPresentButInactive() {
        when(user.isActive()).thenReturn(false);
        String email = "email@localhost";
        when(userSearchService.findUsersByEmail(email)).thenReturn(Collections.singletonList(user));

        Collection<ApplicationUser> result = extractor.getBy(email);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenEmailIsNull() {
        Collection<ApplicationUser> result = extractor.getBy(null);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenEmailIsBlank() {
        Collection<ApplicationUser> result = extractor.getBy("");
        assertThat(result, empty());
    }

    @Test
    void testGetByFetchedFromCacheAfterFirstTimeWhenEmailIsPresent() {
        when(user.isActive()).thenReturn(true);
        String email = "email@localhost";
        when(userSearchService.findUsersByEmail(email)).thenReturn(Collections.singletonList(user));

        extractor.getBy(email);
        extractor.getBy(email);
        Collection<ApplicationUser> result = extractor.getBy(email);

        verify(userSearchService).findUsersByEmail(email);
        assertThat(result, contains(user));
    }
}