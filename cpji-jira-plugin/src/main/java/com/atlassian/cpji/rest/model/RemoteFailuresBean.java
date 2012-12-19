package com.atlassian.cpji.rest.model;

import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @since v2.1
 */
@XmlRootElement (name = "failures")
public class RemoteFailuresBean {
	@XmlElement
	private final List<RemotePluginBean> notInstalled;
	@XmlElement
	private final List<RemotePluginBean> communication;
	@XmlElement
	private final List<RemotePluginBean> authorization;
	@XmlElement
	private final List<RemotePluginBean> authentication;
    @XmlElement
	private final List<RemotePluginBean> unsupported;

	public static RemoteFailuresBean create(@Nonnull final JiraProxyFactory jiraProxyFactory, @Nonnull final String issueId, @Nonnull Iterable<NegativeResponseStatus> responseStatuses) {
		final List<RemotePluginBean> notInstalled = Lists.newArrayList();
		final List<RemotePluginBean> communication = Lists.newArrayList();
		final List<RemotePluginBean> authorization = Lists.newArrayList();
		final List<RemotePluginBean> authentication = Lists.newArrayList();
		final List<RemotePluginBean> unsupported = Lists.newArrayList();

		for(NegativeResponseStatus status : responseStatuses) {
			switch (status.getResult()) {
				case PLUGIN_NOT_INSTALLED:
					notInstalled.add(RemotePluginBean.create(status, jiraProxyFactory, issueId));
					break;
				case AUTHENTICATION_FAILED:
					authentication.add(RemotePluginBean.create(status, jiraProxyFactory, issueId));
					break;
				case AUTHORIZATION_REQUIRED:
					authorization.add(RemotePluginBean.create(status, jiraProxyFactory, issueId));
					break;
				case COMMUNICATION_FAILED:
					communication.add(RemotePluginBean.create(status, jiraProxyFactory, issueId));
					break;
                case UNSUPPORTED_VERSION:
                    unsupported.add(RemotePluginBean.create(status, jiraProxyFactory, issueId));
                    break;
			}
		}
		return new RemoteFailuresBean(notInstalled, communication, authentication, authorization, unsupported);
	}

	public RemoteFailuresBean(Iterable<RemotePluginBean> notInstalled, Iterable<RemotePluginBean> communication,
			Iterable<RemotePluginBean> authentication, Iterable<RemotePluginBean> authorization,
            Iterable<RemotePluginBean> unsupported
    ) {
		this.notInstalled = ImmutableList.copyOf(Preconditions.checkNotNull(notInstalled));
		this.communication = ImmutableList.copyOf(Preconditions.checkNotNull(communication));
		this.authentication = ImmutableList.copyOf(Preconditions.checkNotNull(authentication));
		this.authorization = ImmutableList.copyOf(Preconditions.checkNotNull(authorization));
		this.unsupported = ImmutableList.copyOf(Preconditions.checkNotNull(unsupported));
	}

	@Nonnull
	public List<RemotePluginBean> getNotInstalled() {
		return notInstalled;
	}

	@Nonnull
	public List<RemotePluginBean> getCommunication() {
		return communication;
	}

	@Nonnull
	public List<RemotePluginBean> getAuthorization() {
		return authorization;
	}

	@Nonnull
	public List<RemotePluginBean> getAuthentication() {
		return authentication;
	}
}
