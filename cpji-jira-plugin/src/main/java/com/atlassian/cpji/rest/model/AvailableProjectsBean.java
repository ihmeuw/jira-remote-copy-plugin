package com.atlassian.cpji.rest.model;

import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.Projects;
import com.atlassian.cpji.components.ResponseStatus;
import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 *
 * @since v2.1
 */
@XmlRootElement
public class AvailableProjectsBean {
	@XmlElement
	private List<ProjectGroupBean> projects;
	@XmlElement
	private RemoteFailuresBean failures;

	public AvailableProjectsBean(@Nonnull Iterable<ProjectGroupBean> projects, @Nonnull RemoteFailuresBean failures) {
		this.projects = ImmutableList.copyOf(projects);
		this.failures = failures;
	}

	public static AvailableProjectsBean create(@Nonnull final InternalHostApplication hostApplication, @Nonnull final String issueId, @Nonnull Iterable<Either<ResponseStatus, Projects>> projects) {
		return new AvailableProjectsBean(Iterables.transform(Either.allRight(projects),
				new ProjectsToProjectGroupBean()),
				RemoteFailuresBean.create(hostApplication, issueId, Either.allLeft(projects)));
	}

	private static class ProjectsToProjectGroupBean implements Function<Projects, ProjectGroupBean> {
		@Override
		public ProjectGroupBean apply(final Projects entry) {
			Iterable<BasicProject> basicProjectsIterable = entry.getResult();
			Iterable<ProjectBean> projectsInServer = Iterables
					.transform(basicProjectsIterable, new Function<Object, ProjectBean>() {
						@Override
						public ProjectBean apply(final Object o) {
							return new ProjectBean((BasicProject) o);
						}
					});
			return new ProjectGroupBean(entry.getJiraLocation().getName(), entry.getJiraLocation().getId(), Lists
					.newArrayList(projectsInServer));
		}
	}

	@Nonnull
	public Iterable<ProjectGroupBean> getProjects() {
		return projects;
	}

	@Nonnull
	public RemoteFailuresBean getFailures() {
		return failures;
	}
}
