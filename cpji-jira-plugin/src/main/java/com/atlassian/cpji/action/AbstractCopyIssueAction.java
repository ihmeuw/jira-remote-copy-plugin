package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.EntityLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CommentBean;
import com.atlassian.cpji.rest.model.ComponentBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.cpji.rest.model.TimeTrackingBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.cpji.rest.model.VersionBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.AffectedVersionsSystemField;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.ComponentsSystemField;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.DescriptionSystemField;
import com.atlassian.jira.issue.fields.DueDateSystemField;
import com.atlassian.jira.issue.fields.EnvironmentSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.FixVersionsSystemField;
import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.PrioritySystemField;
import com.atlassian.jira.issue.fields.ReporterSystemField;
import com.atlassian.jira.issue.fields.SecurityLevelSystemField;
import com.atlassian.jira.issue.fields.TimeTrackingSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutStorageException;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericValue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since v1.4
 */
public class AbstractCopyIssueAction extends AbstractIssueSelectAction
{
    public static final String REST_URL_COPY_ISSUE = "/rest/copyissue/latest/";
    public static final String COPY_ISSUE_RESOURCE_PATH = "copyissue";
    public static final String CONVERT_ISSUE_LINKS_RESOURCE_PATH = COPY_ISSUE_RESOURCE_PATH + "/convertIssueLinks";
    public static final String PLUGIN_KEY = "com.atlassian.cpji.cpji-jira-plugin";

    protected String targetEntityLink;

	protected final EntityLinkService entityLinkService;
    protected final FieldLayoutManager fieldLayoutManager;
    protected final CommentManager commentManager;
	protected final ApplicationLinkService applicationLinkService;

    private final FieldManager fieldManager;
    private final FieldMapperFactory fieldMapperFactory;
    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final CopyIssuePermissionManager copyIssuePermissionManager;
    private final UserMappingManager userMappingManager;

    public AbstractCopyIssueAction(final SubTaskManager subTaskManager,
            final EntityLinkService entityLinkService,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final UserMappingManager userMappingManager,
			final ApplicationLinkService applicationLinkService)
    {
        super(subTaskManager);
        this.entityLinkService = entityLinkService;
        this.fieldLayoutManager = fieldLayoutManager;
        this.commentManager = commentManager;
        this.fieldManager = fieldManager;
        this.fieldMapperFactory = fieldMapperFactory;
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.copyIssuePermissionManager = copyIssuePermissionManager;
        this.userMappingManager = userMappingManager;
		this.applicationLinkService = applicationLinkService;
	}

    public SelectedProject getSelectedDestinationProject()
    {
        try
        {
            String[] strings = StringUtils.split(URLDecoder.decode(targetEntityLink, "UTF-8"), "|");
            return new SelectedProject(new ApplicationId(strings[0]), strings[1]);
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new RuntimeException("UTF-8 encoding not supported", ex);
        }
    }

    @SuppressWarnings ("unused")
    public void setTargetEntityLink(String targetEntityLink)
    {
        this.targetEntityLink = targetEntityLink;
    }

    @SuppressWarnings ("unused")
    public String getTargetEntityLink()
    {
        return this.targetEntityLink;
    }

    protected CopyIssueBean createCopyIssueBean(final String targetProjectKey, final MutableIssue issueToCopy, final String targetIssueType)
            throws FieldLayoutStorageException
    {
        CopyIssueBean copyIssueBean = new CopyIssueBean();
        copyIssueBean.setSummary(issueToCopy.getSummary());
        copyIssueBean.setOriginalKey(issueToCopy.getKey());
        copyIssueBean.setBaseUrl(getApplicationProperties().getString(APKeys.JIRA_BASEURL));
        copyIssueBean.setTargetIssueType(targetIssueType);
        copyIssueBean.setTargetProjectKey(targetProjectKey);

        Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(issueToCopy);
        List<String> visibleFieldIds = new ArrayList<String>();
        List<CustomFieldBean> customFieldBeans = new ArrayList<CustomFieldBean>();
        Map<String, FieldMapper> systemFieldMappers = fieldMapperFactory.getSystemFieldMappers();
        for (FieldLayoutItem fieldLayoutItem : fieldLayoutItems)
        {
            OrderableField orderableField = fieldLayoutItem.getOrderableField();
            if (fieldManager.isCustomField(orderableField.getId()))
            {
                CustomField customField = fieldManager.getCustomField(orderableField.getId());
                CustomFieldMapper customFieldMapper = fieldMapperFactory.getCustomFieldMapper().get(customField.getCustomFieldType().getClass().getCanonicalName());
                if (customFieldMapper != null)
                {
                    CustomFieldBean fieldBean = customFieldMapper.createFieldBean(customField, issueToCopy);
                    customFieldBeans.add(fieldBean);
                }
            }
            else
            {
                if (systemFieldMappers.containsKey(orderableField.getId()))
                {
                    visibleFieldIds.add(orderableField.getId());
                }
            }

            if (orderableField instanceof PrioritySystemField)
            {
                copyIssueBean.setPriority(issueToCopy.getPriorityObject().getName());
            }
            else if (orderableField instanceof ReporterSystemField && StringUtils.isNotBlank(issueToCopy.getReporterId()))
            {
                copyIssueBean.setReporter(userMappingManager.createUserBean(issueToCopy.getReporterId()));
            }
            else if (orderableField instanceof EnvironmentSystemField)
            {
                copyIssueBean.setEnvironment(issueToCopy.getEnvironment());
            }
            else if (orderableField instanceof AssigneeSystemField && StringUtils.isNotBlank(issueToCopy.getAssigneeId()))
            {
                copyIssueBean.setAssignee(userMappingManager.createUserBean(issueToCopy.getAssigneeId()));
            }
            else if (orderableField instanceof DescriptionSystemField)
            {
                copyIssueBean.setDescription(issueToCopy.getDescription());
            }
            else if (orderableField instanceof DueDateSystemField)
            {
                copyIssueBean.setIssueDueDate(issueToCopy.getDueDate());
            }
            else if (orderableField instanceof TimeTrackingSystemField && isTimeTrackingEnabled())
            {
                if (issueToCopy.getOriginalEstimate() != null)
                {
                    TimeTrackingBean timeTrackingBean = new TimeTrackingBean(issueToCopy.getOriginalEstimate(), issueToCopy.getTimeSpent(), issueToCopy.getEstimate());
                    copyIssueBean.setTimeTracking(timeTrackingBean);
                }
            }
            else if (orderableField instanceof SecurityLevelSystemField)
            {
                GenericValue securityLevel = issueToCopy.getSecurityLevel();
                if (securityLevel != null)
                {
                    String issueSecurityLevel = securityLevel.getString("name");
                    copyIssueBean.setIssueSecurityLevel(issueSecurityLevel);
                }
            }
            else if (orderableField instanceof LabelsSystemField)
            {
                Set<Label> labels = issueToCopy.getLabels();
                Iterable<String> stringLabels = Iterables.transform(labels, new Function<Label, String>()
                {
                    public String apply(final Label from)
                    {
                        return from.getLabel();
                    }
                });
                copyIssueBean.setLabels(Lists.newArrayList(stringLabels));
            }
            else if (orderableField instanceof ComponentsSystemField)
            {
                Collection<ProjectComponent> componentObjects = issueToCopy.getComponentObjects();
                List<ComponentBean> componentBeans = new ArrayList<ComponentBean>();
                for (ProjectComponent component : componentObjects)
                {
                    ComponentBean componentBean = new ComponentBean(component.getName());
                    componentBeans.add(componentBean);
                }
                copyIssueBean.setComponents(componentBeans);
            }
            else if (orderableField instanceof AffectedVersionsSystemField)
            {
                Collection<Version> affectedVersions = issueToCopy.getAffectedVersions();
                List<VersionBean> affectedVersionBeans = new ArrayList<VersionBean>();
                for (Version affectedVersion : affectedVersions)
                {
                    VersionBean affectedVersionBean = new VersionBean(affectedVersion.getName());
                    affectedVersionBeans.add(affectedVersionBean);
                }
                copyIssueBean.setAffectedVersions(affectedVersionBeans);
            }
            else if (orderableField instanceof FixVersionsSystemField)
            {
                Collection<Version> fixVersions = issueToCopy.getFixVersions();
                List<VersionBean> fixVersionsBean = new ArrayList<VersionBean>();
                for (Version fixVersion : fixVersions)
                {
                    VersionBean fixVersionBean = new VersionBean(fixVersion.getName());
                    fixVersionsBean.add(fixVersionBean);
                }
                copyIssueBean.setFixedForVersions(fixVersionsBean);
            }
        }
        boolean watchingEnabled = getApplicationProperties().getOption(APKeys.JIRA_OPTION_WATCHING);
        if (watchingEnabled)
        {
            List<String> currentWatcherUsernames = getWatcherManager().getCurrentWatcherUsernames(issueToCopy);
            Iterable<UserBean> userBeans = Iterables.transform(currentWatcherUsernames, new Function<String, UserBean>()
            {
                public UserBean apply(final String from)
                {
                    return userMappingManager.createUserBean(from);
                }
            });
            copyIssueBean.setWatchers(Lists.<UserBean>newArrayList(userBeans));
            visibleFieldIds.add(IssueFieldConstants.WATCHERS);
        }

        VoteService voteService = ComponentAccessor.getComponentOfType(VoteService.class);
        ServiceOutcome<Collection<User>> collectionServiceOutcome = voteService.viewVoters(issueToCopy, getLoggedInUser());
        if (collectionServiceOutcome.isValid())
        {
            visibleFieldIds.add(IssueFieldConstants.VOTERS);
            Collection<User> returnedValue = collectionServiceOutcome.getReturnedValue();
            List<UserBean> voters = new ArrayList<UserBean>();
            for (User user : returnedValue)
            {
                voters.add(new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName()));
            }
            copyIssueBean.setVoters(voters);
        }

        copyIssueBean.setVisibleSystemFieldIds(visibleFieldIds);
        copyIssueBean.setCustomFields(customFieldBeans);
        final List<Comment> comments = commentManager.getComments(issueToCopy);
        if (comments != null)
        {
            final List<CommentBean> commentBeans = new ArrayList<CommentBean>();

            for (Comment comment : comments)
            {
                UserBean userBean = userMappingManager.createUserBean(comment.getAuthor());
                CommentBean commentBean = new CommentBean(comment.getBody(), userBean, (comment.getRoleLevel() == null) ? null : comment.getRoleLevel().getName(), comment.getGroupLevel(), comment.getCreated(), comment.getUpdated());
                commentBeans.add(commentBean);
            }
            copyIssueBean.setComments(commentBeans);
        }
        return copyIssueBean;
    }

    protected String checkPermissions()
    {
        try
        {
            final GenericValue issue = getIssue();
            if (!copyIssuePermissionManager.hasPermissionForProject(getIssueObject().getProjectObject().getKey()))
            {
                addErrorMessage(getText("cpji.error.no.permission"));
                return PERMISSION_VIOLATION_RESULT;
            }
            return SUCCESS;
        }
        catch (final IssueNotFoundException ex)
        {
            addErrorMessage(getText("admin.errors.issues.issue.does.not.exist"));
            return ISSUE_NOT_FOUND_RESULT;
        }
        catch (final IssuePermissionException ex)
        {
            addErrorMessage(getText("admin.errors.issues.no.browse.permission"));
            return PERMISSION_VIOLATION_RESULT;
        }
    }

    public String getIssueKey()
    {
        return getIssueObject().getKey();
    }

	public static class SelectedProject {
		private final ApplicationId applicationId;
		private final String projectKey;

		public SelectedProject(ApplicationId applicationId, String projectKey) {
			this.applicationId = applicationId;
			this.projectKey = projectKey;
		}

		public ApplicationId getApplicationId() {
			return applicationId;
		}

		public String getProjectKey() {
			return projectKey;
		}
	}
}
