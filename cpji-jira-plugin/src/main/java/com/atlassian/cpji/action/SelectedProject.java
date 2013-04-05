package com.atlassian.cpji.action;

import com.atlassian.cpji.components.model.JiraLocation;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
*
* @since v6.0
*/
public class SelectedProject {
	@JsonProperty
	private final JiraLocation jiraLocation;
	@JsonProperty
	private final String projectKey;

	@JsonCreator
	public SelectedProject(@JsonProperty("jiraLocation") JiraLocation jiraLocation, @JsonProperty("projectKey") String projectKey) {
		this.jiraLocation = jiraLocation;
		this.projectKey = projectKey;
	}

	public JiraLocation getJiraLocation() {
		return jiraLocation;
	}

	public String getProjectKey() {
		return projectKey;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SelectedProject that = (SelectedProject) o;

		if (jiraLocation != null ? !jiraLocation.equals(that.jiraLocation) : that.jiraLocation != null) return false;
		if (projectKey != null ? !projectKey.equals(that.projectKey) : that.projectKey != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = jiraLocation != null ? jiraLocation.hashCode() : 0;
		result = 31 * result + (projectKey != null ? projectKey.hashCode() : 0);
		return result;
	}
}
