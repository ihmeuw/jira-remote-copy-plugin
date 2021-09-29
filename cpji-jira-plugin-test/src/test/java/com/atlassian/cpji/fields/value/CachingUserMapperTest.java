package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since v3.1
 */
@RunWith(MockitoJUnitRunner.class)
public class CachingUserMapperTest {

    @Mock
    private UserBean userBean;

    @Mock
    private ApplicationUser user;

    @Mock
    private UserSearchService userSearchService;

    private CachingUserMapper cachingUserMapper;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        cachingUserMapper = new CachingUserMapper(userSearchService);
    }

    @Test
    public void testMapUserWhenUserIsNull() throws Exception {
        assertNull(cachingUserMapper.mapUser(null));
    }

    @Test
    public void testGetUsersByEmailWhenEmailIsNull() throws Exception {
        when(userBean.getEmail()).thenReturn(null);
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testGetUsersByEmailWhenEmailIsEmpty() throws Exception {
        when(userBean.getEmail()).thenReturn("");
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testMapUserWhenEmailIsPresent() throws Exception {
        String email = "test@domain.com";
        when(userBean.getEmail()).thenReturn(email);
        ApplicationUser user = mock(ApplicationUser.class);
        when(userSearchService.findUsersByEmail(email)).thenReturn(Collections.singletonList(user));
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertEquals(user, result);
    }

    @Test
    public void testMapUserWhenFullNameIsNull() throws Exception {
        when(userBean.getFullName()).thenReturn(null);
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testMapUserWhenFullNameIsEmpty() throws Exception {
        when(userBean.getFullName()).thenReturn("");
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testMapUserWhenFullNameIsPresent() throws Exception {
        final String fullName = "Jon Sample";
        when(userBean.getFullName()).thenReturn(fullName);
        when(userSearchService.findUsersByFullName(fullName)).thenReturn(Collections.singletonList(user));
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertEquals(user, result);
    }

    @Test
    public void testMapUserWhenUserNameIsNull() throws Exception {
        when(userBean.getUserName()).thenReturn(null);
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testMapUserWhenUserNameIsEmpty() throws Exception {
        when(userBean.getUserName()).thenReturn("");
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertNull(result);
    }

    @Test
    public void testMapUserWhenUserNameIsPresent() throws Exception {
        final String userName = "someuser";
        when(userBean.getUserName()).thenReturn(userName);
        when(userSearchService.getUserByName(null, userName)).thenReturn(user);
        ApplicationUser result = cachingUserMapper.mapUser(userBean);
        assertEquals(user, result);
    }
}
