package com.atlassian.cpji.rest.model;

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
	private final String name;
	@XmlElement
	private final String status;
	@XmlElement
	private final String authorisationUrl;

	public RemotePluginBean(@Nonnull String name, @Nonnull String status, @Nullable String authorisationUrl) {
		this.name = name;
		this.status = status;
		this.authorisationUrl = authorisationUrl;
	}

	@Nonnull
	public String getName() {
		return name;
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
