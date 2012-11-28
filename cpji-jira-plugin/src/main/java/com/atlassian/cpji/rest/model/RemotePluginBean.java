package com.atlassian.cpji.rest.model;

import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.cpji.rest.RemotesResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @since v2.1
 */
@XmlRootElement (name = "remotePlugin")
public class RemotePluginBean {

	@XmlElement
	private final String applicationName;
	@XmlElement
	private final String applicationId;
	@XmlElement
	private final String status;
	@XmlElement
	private final String authorisationUrl;

	public static RemotePluginBean create(@Nonnull ResponseStatus input, @Nonnull InternalHostApplication hostApplication, @Nonnull String issueId) {
		return new RemotePluginBean(input.getApplicationLink().getName(),
				input.getApplicationLink().getId().get(),
				input.getStatus().toString(),
				RemotesResource.generateAuthorizationUrl(hostApplication,
						input.getApplicationLink().createAuthenticatedRequestFactory(), issueId));
	}

	public RemotePluginBean(@Nonnull String name, @Nonnull String id, @Nonnull String status, @Nullable String authorisationUrl) {
		this.applicationName = name;
		this.applicationId = id;
		this.status = status;
		this.authorisationUrl = authorisationUrl;
	}

	@Nonnull
	public String getApplicationId() {
		return applicationId;
	}

	@Nonnull
	public String getApplicationName() {
		return applicationName;
	}

	@Nonnull
	public String getStatus() {
		return status;
	}

	@Nullable
	public String getAuthorisationUrl() {
		return authorisationUrl;
	}
}
