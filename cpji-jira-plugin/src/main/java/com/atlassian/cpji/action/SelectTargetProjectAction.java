package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.RecentlyUsedProjectsManager;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.PluginVersion;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.atlassian.fugue.Either;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @since v1.2
 */
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class SelectTargetProjectAction extends AbstractCopyIssueAction
{
	private final RecentlyUsedProjectsManager recentlyUsedProjectsManager;

	public SelectTargetProjectAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
			final ApplicationLinkService applicationLinkService,
            final WebResourceManager webResourceManager,
            final JiraProxyFactory jiraProxyFactory,
            final IssueLinkTypeManager issueLinkTypeManager,
			final RecentlyUsedProjectsManager recentlyUsedProjectsManager)
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory, webResourceManager, issueLinkTypeManager);
		this.recentlyUsedProjectsManager = recentlyUsedProjectsManager;

		webResourceManager.requireResource(PLUGIN_KEY + ":selectTargetProjectAction");
		setCurrentStep("selectproject");
	}

    @Override
    public String doDefault() throws Exception
    {
        final String result = checkPermissions();
		if (SUCCESS.equals(result) && recentlyUsedProjectsManager.getRecentProjects(getLoggedInUser()).isEmpty()) {
			recentlyUsedProjectsManager.addProject(getLoggedInUser(), new SelectedProject(JiraLocation.LOCAL, getSelectedIssueProjectKey()));
		}
		return result;
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception
    {
		String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        final SelectedProject selectedEntityLink = getSelectedDestinationProject();
        JiraProxy jira = jiraProxyFactory.createJiraProxy(selectedEntityLink.getJiraLocation());
        Either<NegativeResponseStatus,PluginVersion> response = jira.isPluginInstalled();
        PluginVersion result = handleGenericResponseStatus(jira, response, null);
        if(result == null) {
            return getGenericResponseHandlerResult();
        }

		recentlyUsedProjectsManager.addProject(getLoggedInUser(), selectedEntityLink);
        return getRedirect("/secure/CopyDetailsAction!default.jspa?id=" + getId() + "&targetEntityLink=" + targetEntityLink, true);
    }

    @SuppressWarnings("unused")
    public String getSelectedIssueProjectKey(){
        return getIssueObject().getProjectObject().getKey();
    }

	public String getRecentlyUsedProjects() {
		final List<SelectedProject> recentProjects = recentlyUsedProjectsManager.getRecentProjects(getLoggedInUser());
		final Map<String, List<String>> model = Maps.newHashMap(); // not using Multimap here because I'd have to install mapper for jackson
		for (SelectedProject project : recentProjects) {
			final String locationId = project.getJiraLocation().getId();
			if (!model.containsKey(locationId)) {
				model.put(locationId, Lists.<String>newArrayList());
			}
			model.get(locationId).add(project.getProjectKey());
		}
		try {
			return new ObjectMapper().writeValueAsString(model);
		} catch (IOException e) {
			return "{}";
		}
	}


}
