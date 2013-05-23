package com.atlassian.cpji.fields.value;

import com.atlassian.core.util.StringUtils;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 *
 * @since v3.1
 */
@RunWith(MockitoJUnitRunner.class)
public class CachingUserMapperTest {

    @MockitoAnnotations.Mock
    private UserBean userBean;

    @MockitoAnnotations.Mock
    private User user;

    @MockitoAnnotations.Mock
    private Multimap<String,User> multimap;

    private CachingUserMapper cachingUserMapper;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        cachingUserMapper = new CachingUserMapper(Collections.EMPTY_LIST);
    }

    @Test
    public void testGetUsersByEmailWhenEmailIsNull() throws Exception {
        when(userBean.getEmail()).thenReturn(null);
        final Multimap<String, User> usersByEmail = cachingUserMapper.getUsersByEmail(userBean, multimap);
        Assert.assertNull(usersByEmail);
    }

    @Test
    public void testGetUsersByEmailWhenEmailIsEmpty() throws Exception {
        when(userBean.getEmail()).thenReturn("");
        final Multimap<String, User> usersByEmail = cachingUserMapper.getUsersByEmail(userBean, multimap);
        Assert.assertNull(usersByEmail);
    }
    @Test
    public void testGetUsersByEmailWhenEmailIsPresent() throws Exception {
        when(userBean.getEmail()).thenReturn("test@domain.com");
        final Multimap<String, User> usersByEmail = cachingUserMapper.getUsersByEmail(userBean, multimap);
        Assert.assertNotNull(usersByEmail);
    }

    @Test
    public void testGetUsersByFullNameWhenFullNameIsNull() throws Exception {
        when(userBean.getFullName()).thenReturn(null);
        final Multimap<String, User> usersByFullName = cachingUserMapper.getUsersByFullName(userBean, multimap);
        Assert.assertNull(usersByFullName);
    }

    @Test
    public void testGetUsersByFullNameWhenFullNameIsEmpty() throws Exception {
        when(userBean.getFullName()).thenReturn("");
        final Multimap<String, User> usersByFullName = cachingUserMapper.getUsersByFullName(userBean, multimap);
        Assert.assertNull(usersByFullName);
    }

    @Test
    public void testGetUsersByFullNameWhenFullNameIsPresent() throws Exception {
        when(userBean.getFullName()).thenReturn("Jon Sample");
        final Multimap<String, User> usersByFullName = cachingUserMapper.getUsersByFullName(userBean, multimap);
        Assert.assertNotNull(usersByFullName);
    }

    @Test
    public void testGetUsersByUserNameWhenUserNameIsNull() throws Exception {
        when(userBean.getUserName()).thenReturn(null);
        final Collection<User> usersByUserName = cachingUserMapper.getUsersByUserName(userBean, multimap);
        Assert.assertEquals(usersByUserName, Collections.emptyList());
    }

    @Test
    public void testGetUsersByUserNameWhenUserNameIsEmpty() throws Exception {
        when(userBean.getUserName()).thenReturn("");
        final Collection<User> usersByUserName = cachingUserMapper.getUsersByUserName(userBean, multimap);
        Assert.assertEquals(usersByUserName, Collections.emptyList());
    }

    @Test
    public void testGetUsersByUserNameWhenUserNameIsPresent() throws Exception {
        when(userBean.getUserName()).thenReturn("someuser");
        when(multimap.get("someuser")).thenReturn(ImmutableList.of(user));
        final Collection<User> usersByUserName = cachingUserMapper.getUsersByUserName(userBean, multimap);
        Assert.assertEquals(usersByUserName, ImmutableList.of(user));
    }
}
