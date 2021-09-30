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
public class UsersByFullNameExtractorTest {

    @Mock
    private UserSearchService userSearchService;

    @Mock
    private ApplicationUser user;

    private CachedExtractor extractor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        extractor = new UsersByFullNameExtractor(userSearchService);
    }

    @Test
    void testGetByWhenFullNameIsPresent() {
        when(user.isActive()).thenReturn(true);
        String fullName = "Full Name";
        when(userSearchService.findUsersByFullName(fullName)).thenReturn(Collections.singletonList(user));

        Collection<ApplicationUser> result = extractor.getBy(fullName);
        assertThat(result, contains(user));
    }

    @Test
    void testGetByWhenFullNameIsPresentButInactive() {
        when(user.isActive()).thenReturn(false);
        String fullName = "Full Name";
        when(userSearchService.findUsersByFullName(fullName)).thenReturn(Collections.singletonList(user));

        Collection<ApplicationUser> result = extractor.getBy(fullName);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenFullNameIsNull() {
        Collection<ApplicationUser> result = extractor.getBy(null);
        assertThat(result, empty());
    }

    @Test
    void testGetByWhenFullNameIsBlank() {
        Collection<ApplicationUser> result = extractor.getBy("");
        assertThat(result, empty());
    }

    @Test
    void testGetByFetchedFromCacheAfterFirstTimeWhenFullNameIsPresent() {
        when(user.isActive()).thenReturn(true);
        String fullName = "Full Name";
        when(userSearchService.findUsersByFullName(fullName)).thenReturn(Collections.singletonList(user));

        extractor.getBy(fullName);
        extractor.getBy(fullName);
        Collection<ApplicationUser> result = extractor.getBy(fullName);

        verify(userSearchService).findUsersByFullName(fullName);
        assertThat(result, contains(user));
    }
}