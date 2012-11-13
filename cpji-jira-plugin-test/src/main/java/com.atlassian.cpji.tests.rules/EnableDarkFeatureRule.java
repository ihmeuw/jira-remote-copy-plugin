package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;
import com.google.common.collect.ImmutableList;
import org.junit.rules.ExternalResource;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Properties;

public class EnableDarkFeatureRule extends ExternalResource {
	private final ImmutableList<JiraTestedProduct> jiras;
	private final String feature;
	private boolean disableAfter = false;

	public <T extends JiraTestedProduct> EnableDarkFeatureRule(@Nonnull String s, T...jiras) {
		this.feature = s;
		this.jiras = ImmutableList.<JiraTestedProduct>copyOf(jiras);
	}

	public EnableDarkFeatureRule andDisableAfter() {
		this.disableAfter = true;
		return this;
	}

	@Override
	protected void before() throws Throwable {
		super.before();
		for(JiraTestedProduct jira : jiras) {
			getBackdoor(jira).darkFeatures().enableForSite(feature);
		}
	}

	@Override
	protected void after() {
		if (this.disableAfter) {
			for(JiraTestedProduct jira : jiras) {
				getBackdoor(jira).darkFeatures().disableForSite(feature);
			}
		}
		super.after();
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