package com.atlassian.cpji.tests;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;

import java.net.URI;
import java.util.Properties;

public final class BackdoorFactory
{
    private BackdoorFactory()
    {
        throw new AssertionError("Don't instantiate me");
    }

	public static Backdoor getBackdoor(JiraTestedProduct jira) {
		Properties props = new Properties();
		props.put("jira.port", Integer.toString(jira.getProductInstance().getHttpPort()));
		props.put("jira.context", jira.getProductInstance().getContextPath());
		props.put("jira.host", URI.create(jira.getProductInstance().getBaseUrl()).getHost());
		props.put("jira.xml.data.location", ".");
		return new Backdoor(new TestKitLocalEnvironmentData(props, null));
	}
}