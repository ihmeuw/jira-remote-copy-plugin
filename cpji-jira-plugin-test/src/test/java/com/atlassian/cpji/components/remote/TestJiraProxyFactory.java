package com.atlassian.cpji.components.remote;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.google.common.base.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestJiraProxyFactory {

    private JiraProxyFactory proxyFactory;

    @Mock
    private ApplicationLinkService applicationLinkService;

    @Before
    public void setUp(){
        proxyFactory = new JiraProxyFactory(applicationLinkService, null, null, null, null, null, null, null, null, null, null, null, null);
    }


    @Test
    public void testIsLocalLocation(){
        Predicate<JiraLocation> predicate = JiraLocation.isLocalLocation();
        ApplicationProperties ap = mock(ApplicationProperties.class);
        when(ap.getString(APKeys.JIRA_TITLE)).thenReturn("MyLocalJira");
        LocalJiraProxy localProxy = new LocalJiraProxy(null, null, null, null, null, null, null, null, null, ap);
        assertTrue(predicate.apply(localProxy.getJiraLocation()));

        RemoteJiraProxy remoteProxy = new RemoteJiraProxy(null, null, new JiraLocation("MyAwesomeRemoteJira", null), null, null, null);
        assertFalse(predicate.apply(remoteProxy.getJiraLocation()));

        assertFalse(predicate.apply(null));
    }





}
