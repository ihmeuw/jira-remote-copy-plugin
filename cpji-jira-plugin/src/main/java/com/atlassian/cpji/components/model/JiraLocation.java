package com.atlassian.cpji.components.model;

import com.atlassian.applinks.api.ApplicationLink;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.annotation.Nullable;

/**
 * @since v3.0
 */
public class JiraLocation {

	public static final String LOCAL_ID = new String("LOCAL");
	public static final JiraLocation LOCAL = new JiraLocation(LOCAL_ID, LOCAL_ID);

	@JsonProperty
	private final String id;

	@JsonProperty
    private final String name;

	public static Predicate<JiraLocation> isLocalLocation() {
		return new Predicate<JiraLocation>() {
			@Override
			public boolean apply(@Nullable JiraLocation input) {
				if (input == null)
					return false;
				return input.isLocal();
			}
		};
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraLocation that = (JiraLocation) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

	@JsonCreator
    public JiraLocation(@JsonProperty("id") final String id, @JsonProperty("name") final String name) {
        this.id = Preconditions.checkNotNull(id);
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
	}

	@JsonIgnore
	public boolean isLocal() {
		return id.equalsIgnoreCase(JiraLocation.LOCAL_ID);
	}

    public static JiraLocation fromAppLink(ApplicationLink appLink) {
        return new JiraLocation(appLink.getId().get(), appLink.getName());
    }

}
