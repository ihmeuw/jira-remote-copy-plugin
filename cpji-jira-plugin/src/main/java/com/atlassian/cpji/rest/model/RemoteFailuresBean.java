package com.atlassian.cpji.rest.model;

import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.ResponseStatus;
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

	public static RemoteFailuresBean create(@Nonnull final InternalHostApplication hostApplication, @Nonnull final String issueId, @Nonnull Iterable<ResponseStatus> responseStatuses) {
		final List<RemotePluginBean> notInstalled = Lists.newArrayList();
		final List<RemotePluginBean> communication = Lists.newArrayList();
		final List<RemotePluginBean> authorization = Lists.newArrayList();
		final List<RemotePluginBean> authentication = Lists.newArrayList();

		for(ResponseStatus status : responseStatuses) {
			switch (status.getStatus()) {
				case PLUGIN_NOT_INSTALLED:
					notInstalled.add(RemotePluginBean.create(status, hostApplication, issueId));
					break;
				case AUTHENTICATION_FAILED:
					authentication.add(RemotePluginBean.create(status, hostApplication, issueId));
					break;
				case AUTHORIZATION_REQUIRED:
					authorization.add(RemotePluginBean.create(status, hostApplication, issueId));
					break;
				case COMMUNICATION_FAILED:
					communication.add(RemotePluginBean.create(status, hostApplication, issueId));
					break;
			}
		}
		return new RemoteFailuresBean(notInstalled, communication, authentication, authorization);
	}

	public RemoteFailuresBean(@Nonnull Iterable<RemotePluginBean> notInstalled, @Nonnull Iterable<RemotePluginBean> communication,
			@Nonnull Iterable<RemotePluginBean> authentication, @Nonnull Iterable<RemotePluginBean> authorization) {
		this.notInstalled = ImmutableList.copyOf(notInstalled);
		this.communication = ImmutableList.copyOf(communication);
		this.authentication = ImmutableList.copyOf(authentication);
		this.authorization = ImmutableList.copyOf(authorization);
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
