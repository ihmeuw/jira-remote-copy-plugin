package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.ResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.apache.log4j.Logger;

/**
 * @since v1.2
 */
public class SelectTargetProjectAction extends AbstractCopyIssueAction
{

    public static final String AUTHORIZE = "authorize";

	private static final Logger log = Logger.getLogger(SelectTargetProjectAction.class);
    private String authorizationUrl;

	public SelectTargetProjectAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager,
			final ApplicationLinkService applicationLinkService,
            final WebResourceManager webResourceManager,
            final JiraProxyFactory jiraProxyFactory
            )
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory,
				fieldLayoutItemsRetriever, copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory, webResourceManager);
        webResourceManager.requireResource(PLUGIN_KEY + ":selectTargetProjectAction");
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
        final SelectedProject selectedEntityLink;
        try
        {
            selectedEntityLink = getSelectedDestinationProject();
        }
        catch (Exception ex)
        {
            log.error("Failed to find entity link", ex);
            addErrorMessage("Failed to find entity link! Reason: '" + targetEntityLink + ex.getMessage() + "'");
            return ERROR;
        }

        JiraProxy jira = jiraProxyFactory.createJiraProxy(selectedEntityLink.getJiraLocation());
        Either<ResponseStatus,SuccessfulResponse> response = jira.isPluginInstalled();
        if(response.isLeft()){
            ResponseStatus responseStatus = (ResponseStatus) response.left().get();
            if (ResponseStatus.Status.AUTHORIZATION_REQUIRED.equals(responseStatus.getResult()))
            {
                authorizationUrl = jira.generateAuthenticationUrl(Long.toString(id));
                return AUTHORIZE;
            }
            else if (ResponseStatus.Status.PLUGIN_NOT_INSTALLED.equals(responseStatus.getResult()))
            {
                log.warn("Remote JIRA instance does NOT have the CPJI plugin installed.");
                addErrorMessage("Remote JIRA instance does NOT have the CPJI plugin installed.");
            }
            else if (ResponseStatus.Status.AUTHENTICATION_FAILED.equals(responseStatus.getResult()))
            {
                addErrorMessage("Authentication failed. Check the authentication configuration.");
            }
            return ERROR;
        }
        return getRedirect("/secure/CopyDetailsAction.jspa?id=" + getId() + "&targetEntityLink=" + targetEntityLink);
    }


    public String getAuthorizationUrl()
    {
        return authorizationUrl;
    }


}
