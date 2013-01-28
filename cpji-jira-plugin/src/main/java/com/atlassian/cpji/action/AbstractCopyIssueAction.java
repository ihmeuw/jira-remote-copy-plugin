package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;

/**
 * @since v1.4
 */
public abstract class AbstractCopyIssueAction extends AbstractIssueSelectAction {
    public static final String AUTHORIZE = "authorize";

	public static final String PLUGIN_KEY = "com.atlassian.cpji.cpji-jira-plugin";
	public static final String RESOURCES_ADMIN_JS = PLUGIN_KEY + ":admin-js";

	protected String targetEntityLink;

	protected final FieldLayoutManager fieldLayoutManager;
	protected final CommentManager commentManager;
	protected final ApplicationLinkService applicationLinkService;
	protected final JiraProxyFactory jiraProxyFactory;
	
	private String genericResponseHandlerResult = ERROR;
	private String authorizationUrl;
	private String currentStep;

	private final CopyIssuePermissionManager copyIssuePermissionManager;
    private final IssueLinkTypeManager issueLinkTypeManager;

    public AbstractCopyIssueAction(final SubTaskManager subTaskManager,
                                   final FieldLayoutManager fieldLayoutManager,
                                   final CommentManager commentManager,
                                   final CopyIssuePermissionManager copyIssuePermissionManager,
                                   final ApplicationLinkService applicationLinkService,
                                   final JiraProxyFactory jiraProxyFactory,
                                   final WebResourceManager webResourceManager,
                                   final IssueLinkTypeManager issueLinkTypeManager) {
		super(subTaskManager);
		this.fieldLayoutManager = fieldLayoutManager;
		this.commentManager = commentManager;
		this.copyIssuePermissionManager = copyIssuePermissionManager;
		this.applicationLinkService = applicationLinkService;
		this.jiraProxyFactory = jiraProxyFactory;
        this.issueLinkTypeManager = issueLinkTypeManager;

        webResourceManager.requireResourcesForContext("com.atlassian.cpji.cpji-jira-plugin.copy-context");
	}

	public SelectedProject getSelectedDestinationProject() {
		try {
			String[] strings = StringUtils.split(URLDecoder.decode(targetEntityLink, "UTF-8"), "|");
			return new SelectedProject(jiraProxyFactory.getLocationById(strings[0]), strings[1]);
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("UTF-8 encoding not supported", ex);
		}
	}

	public boolean isTargetLocal() {
		return getSelectedDestinationProject().getJiraLocation().isLocal();
	}

    protected <T> T handleGenericResponseStatus(JiraProxy jira, Either<NegativeResponseStatus, T> response, Function<NegativeResponseStatus, Void> errorOccuredHandler){
        if(response.isRight()){
            return response.right().get();
        } else {
            NegativeResponseStatus status = response.left().get();
            log.error(status);
            switch(status.getResult()){
                case AUTHENTICATION_FAILED:
                    addErrorMessage(getText("cpji.errors.authentication.failed"));
                    break;
                case AUTHORIZATION_REQUIRED:
                    addErrorMessage(getText("cpji.errors.oauth.token.invalid"));
                    authorizationUrl = jira.generateAuthenticationUrl(Long.toString(id));
                    genericResponseHandlerResult = AUTHORIZE;
                    break;
                case PLUGIN_NOT_INSTALLED:
                    addErrorMessage(getText("cpji.errors.plugin.not.installed"));
                    break;
                case COMMUNICATION_FAILED:
                    addErrorMessage(getText("cpji.errors.communication.failed"));
                    break;
                case ERROR_OCCURRED:
                    if(errorOccuredHandler != null){
                        errorOccuredHandler.apply(status);
                    } else {
                        addErrorMessage(getText("cpji.errors.error.occured.generic"));
                    }
            }
            return null;
        }

    }

	@SuppressWarnings("unused")
	public void setTargetEntityLink(String targetEntityLink) {
		this.targetEntityLink = targetEntityLink;
	}

	@SuppressWarnings("unused")
	public String getTargetEntityLink() {
		return this.targetEntityLink;
	}

	protected String checkPermissions() {
		try {
			if (!copyIssuePermissionManager.hasPermissionForProject(getIssueObject().getProjectObject().getKey())) {
				addErrorMessage(getText("cpji.error.no.permission"));
				return PERMISSION_VIOLATION_RESULT;
			}
			return SUCCESS;
		} catch (final IssueNotFoundException ex) {
			addErrorMessage(getText("admin.errors.issues.issue.does.not.exist"));
			return ISSUE_NOT_FOUND_RESULT;
		} catch (final IssuePermissionException ex) {
			addErrorMessage(getText("admin.errors.issues.no.browse.permission"));
			return PERMISSION_VIOLATION_RESULT;
		}
	}

	public String getIssueKey() {
		return getIssueObject().getKey();
	}

    public String getAuthorizationUrl(){
        return authorizationUrl;
    }

    public String getGenericResponseHandlerResult(){
        return genericResponseHandlerResult;
    }

	public static class SelectedProject {
		private final JiraLocation jiraLocation;
		private final String projectKey;

		public SelectedProject(JiraLocation jiraLocation, String projectKey) {
			this.jiraLocation = jiraLocation;
			this.projectKey = projectKey;
		}

		public JiraLocation getJiraLocation() {
			return jiraLocation;
		}

		public String getProjectKey() {
			return projectKey;
		}
	}

	public void setCurrentStep(String name) {
		Preconditions.checkNotNull(name);
		this.currentStep = name;
	}

	public String getCurrentStep() {
		return this.currentStep;
	}

    /**
     * Returns the issue link type specified by the clone link name in the properties file or null for none.
     *
     * @return the issue link type specified by the clone link name in the properties file or null for none.
     */
    IssueLinkType getCloneIssueLinkType()
    {
        String cloneIssueLinkTypeName = getCloneIssueLinkTypeName();
        if (cloneIssueLinkTypeName == null)
            return null;
        final Collection<IssueLinkType> cloneIssueLinkTypes = issueLinkTypeManager.getIssueLinkTypesByName(cloneIssueLinkTypeName);

        if (cloneIssueLinkTypes.isEmpty())
        {
            return null;
        }
        else
        {
            return cloneIssueLinkTypes.iterator().next();
        }
    }

    String getCloneIssueLinkTypeName()
    {
        return getApplicationProperties().getDefaultBackedString(APKeys.JIRA_CLONE_LINKTYPE_NAME);
    }


}
