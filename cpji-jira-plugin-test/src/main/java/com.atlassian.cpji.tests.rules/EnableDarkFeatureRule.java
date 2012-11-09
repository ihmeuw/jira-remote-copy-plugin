package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.google.common.collect.ImmutableList;
import org.junit.rules.ExternalResource;

import javax.annotation.Nonnull;

public class EnableDarkFeatureRule extends ExternalResource {
	private final ImmutableList<JiraTestedProduct> jiras;
	private final String feature;

	public <T extends JiraTestedProduct> EnableDarkFeatureRule(@Nonnull String s, T...jiras) {
		this.feature = s;
		this.jiras = ImmutableList.<JiraTestedProduct>copyOf(jiras);
	}

	@Override
	protected void before() throws Throwable {
		super.before();
		for(JiraTestedProduct jira : jiras) {
			jira.backdoor().darkFeatures().enableForSite(feature);
		}
	}

	@Override
	protected void after() {
		for(JiraTestedProduct jira : jiras) {
			jira.backdoor().darkFeatures().disableForSite(feature);
		}
		super.after();
	}
}