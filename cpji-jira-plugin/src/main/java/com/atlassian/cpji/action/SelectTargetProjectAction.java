package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.PluginVersion;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;

/**
 * @since v1.2
 */
public class SelectTargetProjectAction extends AbstractCopyIssueAction
{
	public SelectTargetProjectAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
			final ApplicationLinkService applicationLinkService,
            final WebResourceManager webResourceManager,
            final JiraProxyFactory jiraProxyFactory,
            final IssueLinkTypeManager issueLinkTypeManager
            )
    {
        super(subTaskManager, fieldLayoutManager, commentManager,
				copyIssuePermissionManager, applicationLinkService, jiraProxyFactory, webResourceManager, issueLinkTypeManager);

        webResourceManager.requireResource(PLUGIN_KEY + ":selectTargetProjectAction");
		setCurrentStep("selectproject");
	}

    @Override
    public String doDefault() throws Exception
    {
        return checkPermissions();
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
        final SelectedProject selectedEntityLink= getSelectedDestinationProject();
        JiraProxy jira = jiraProxyFactory.createJiraProxy(selectedEntityLink.getJiraLocation());
        Either<NegativeResponseStatus,PluginVersion> response = jira.isPluginInstalled();
        PluginVersion result = handleGenericResponseStatus(jira, response, null);
        if(result == null){
            return getGenericResponseHandlerResult();
        }

        return getRedirect("/secure/CopyDetailsAction!default.jspa?id=" + getId() + "&targetEntityLink=" + targetEntityLink);
    }

    @SuppressWarnings("unused")
    public String getSelectedIssueProjectKey(){
        return getIssueObject().getProjectObject().getKey();
    }


}
