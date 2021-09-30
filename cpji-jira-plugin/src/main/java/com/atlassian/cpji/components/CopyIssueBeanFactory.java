package com.atlassian.cpji.components;

import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.SystemFieldMapper;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CommentBean;
import com.atlassian.cpji.rest.model.ComponentBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.TimeTrackingBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.cpji.rest.model.VersionBean;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
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
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
public class CopyIssueBeanFactory {
	private final FieldManager fieldManager;
	private final FieldMapperFactory fieldMapperFactory;
	private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
	private final UserMappingManager userMappingManager;
	private final ApplicationProperties applicationProperties;
	private final WatcherManager watcherManager;
	private final JiraAuthenticationContext authenticationContext;
	private final CommentManager commentManager;
	private final TimeTrackingConfiguration timeTrackingConfiguration;
	private final VoteService voteService;

	public CopyIssueBeanFactory(FieldManager fieldManager, FieldMapperFactory fieldMapperFactory,
			FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
			UserMappingManager userMappingManager, ApplicationProperties applicationProperties,
			WatcherManager watcherManager, JiraAuthenticationContext authenticationContext, CommentManager commentManager,
			TimeTrackingConfiguration timeTrackingConfiguration,
			VoteService voteService) {
		this.fieldManager = fieldManager;
		this.fieldMapperFactory = fieldMapperFactory;
		this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
		this.userMappingManager = userMappingManager;
		this.applicationProperties = applicationProperties;
		this.watcherManager = watcherManager;
		this.authenticationContext = authenticationContext;
		this.commentManager = commentManager;
		this.timeTrackingConfiguration = timeTrackingConfiguration;
		this.voteService = voteService;
	}

	public CopyIssueBean create(final String targetProjectKey, final MutableIssue issueToCopy, final String targetIssueType, final String newSummary,
			boolean copyComments) {
		Preconditions.checkNotNull(targetProjectKey);
		Preconditions.checkNotNull(issueToCopy);
		Preconditions.checkNotNull(targetIssueType);

		CopyIssueBean copyIssueBean = new CopyIssueBean();
		copyIssueBean.setSummary(newSummary);
		copyIssueBean.setOriginalKey(issueToCopy.getKey());
		copyIssueBean.setBaseUrl(applicationProperties.getString(APKeys.JIRA_BASEURL));
		copyIssueBean.setTargetIssueType(targetIssueType);
		copyIssueBean.setTargetProjectKey(targetProjectKey);

		Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever
				.getAllVisibleFieldLayoutItems(issueToCopy);
		List<String> visibleFieldIds = new ArrayList<String>();
		List<CustomFieldBean> customFieldBeans = new ArrayList<CustomFieldBean>();
		Map<String, SystemFieldMapper> systemFieldMappers = fieldMapperFactory.getSystemFieldMappers();
		for (FieldLayoutItem fieldLayoutItem : fieldLayoutItems) {
			OrderableField orderableField = fieldLayoutItem.getOrderableField();
			if (fieldManager.isCustomField(orderableField.getId())) {
				CustomField customField = fieldManager.getCustomField(orderableField.getId());
                if(customField.hasValue(issueToCopy)){
                    CustomFieldMapper customFieldMapper = fieldMapperFactory.getCustomFieldMapper(customField.getCustomFieldType());
                    if (customFieldMapper != null) {
                        CustomFieldBean fieldBean = customFieldMapper.createFieldBean(customField, issueToCopy);
                        customFieldBeans.add(fieldBean);
                    } else {
                        copyIssueBean.addUnsupportedCustomField(new CustomFieldPermissionBean(customField.getId(), customField.getName(),
                                ValidationCode.FIELD_TYPE_NOT_SUPPORTED.toString(),
                                Collections.<String>emptyList()));
                    }
                }
			} else {
				if (systemFieldMappers.containsKey(orderableField.getId())) {
					visibleFieldIds.add(orderableField.getId());
				}
			}

			if (orderableField instanceof PrioritySystemField) {
				if (issueToCopy.getPriorityObject() != null) {
					copyIssueBean.setPriority(issueToCopy.getPriorityObject().getName());
				}
			} else if (orderableField instanceof ReporterSystemField && StringUtils
					.isNotBlank(issueToCopy.getReporterId())) {
				copyIssueBean.setReporter(userMappingManager.createUserBean(issueToCopy.getReporter().getKey()));
			} else if (orderableField instanceof EnvironmentSystemField) {
				copyIssueBean.setEnvironment(issueToCopy.getEnvironment());
			} else if (orderableField instanceof AssigneeSystemField && StringUtils
					.isNotBlank(issueToCopy.getAssigneeId())) {
				copyIssueBean.setAssignee(userMappingManager.createUserBean(issueToCopy.getAssignee().getKey()));
			} else if (orderableField instanceof DescriptionSystemField) {
				copyIssueBean.setDescription(issueToCopy.getDescription());
			} else if (orderableField instanceof DueDateSystemField) {
				copyIssueBean.setIssueDueDate(issueToCopy.getDueDate());
			} else if (orderableField instanceof TimeTrackingSystemField && isTimeTrackingEnabled()) {
				if (issueToCopy.getOriginalEstimate() != null) {
					TimeTrackingBean timeTrackingBean = new TimeTrackingBean(issueToCopy.getOriginalEstimate(),
							issueToCopy.getTimeSpent(), issueToCopy.getEstimate());
					copyIssueBean.setTimeTracking(timeTrackingBean);
				}
			} else if (orderableField instanceof SecurityLevelSystemField) {
				GenericValue securityLevel = issueToCopy.getSecurityLevel();
				if (securityLevel != null) {
					String issueSecurityLevel = securityLevel.getString("name");
					copyIssueBean.setIssueSecurityLevel(issueSecurityLevel);
				}
			} else if (orderableField instanceof LabelsSystemField) {
				Set<Label> labels = issueToCopy.getLabels();
				Iterable<String> stringLabels = Iterables.transform(labels, new Function<Label, String>() {
					public String apply(final Label from) {
						return from.getLabel();
					}
				});
				copyIssueBean.setLabels(Lists.newArrayList(stringLabels));
			} else if (orderableField instanceof ComponentsSystemField) {
				Collection<ProjectComponent> componentObjects = issueToCopy.getComponentObjects();
				List<ComponentBean> componentBeans = new ArrayList<ComponentBean>();
				for (ProjectComponent component : componentObjects) {
					ComponentBean componentBean = new ComponentBean(component.getName());
					componentBeans.add(componentBean);
				}
				copyIssueBean.setComponents(componentBeans);
			} else if (orderableField instanceof AffectedVersionsSystemField) {
				Collection<Version> affectedVersions = issueToCopy.getAffectedVersions();
				List<VersionBean> affectedVersionBeans = new ArrayList<VersionBean>();
				for (Version affectedVersion : affectedVersions) {
					VersionBean affectedVersionBean = new VersionBean(affectedVersion.getName());
					affectedVersionBeans.add(affectedVersionBean);
				}
				copyIssueBean.setAffectedVersions(affectedVersionBeans);
			} else if (orderableField instanceof FixVersionsSystemField) {
				Collection<Version> fixVersions = issueToCopy.getFixVersions();
				List<VersionBean> fixVersionsBean = new ArrayList<VersionBean>();
				for (Version fixVersion : fixVersions) {
					VersionBean fixVersionBean = new VersionBean(fixVersion.getName());
					fixVersionsBean.add(fixVersionBean);
				}
				copyIssueBean.setFixedForVersions(fixVersionsBean);
			}
		}

		setVoters(copyIssueBean, issueToCopy, visibleFieldIds);

		copyIssueBean.setVisibleSystemFieldIds(visibleFieldIds);
		copyIssueBean.setCustomFields(customFieldBeans);

		if (copyComments) {
			setComments(copyIssueBean, issueToCopy);
		}

		return copyIssueBean;
	}

	private void setComments(CopyIssueBean copyIssueBean, MutableIssue issueToCopy) {
		final List<Comment> comments = commentManager.getComments(issueToCopy);
		if (comments != null) {
			final List<CommentBean> commentBeans = new ArrayList<CommentBean>();

			for (Comment comment : comments) {
				UserBean userBean = userMappingManager.createUserBean(comment.getAuthorApplicationUser().getKey());
				if (userBean != null) {
					final CommentBean commentBean = new CommentBean(comment.getBody(), userBean,
							(comment.getRoleLevel() == null) ? null : comment.getRoleLevel().getName(),
							comment.getGroupLevel(), comment.getCreated(), comment.getUpdated());
					commentBeans.add(commentBean);
				}
			}
			copyIssueBean.setComments(commentBeans);
		}
	}

	private void setVoters(CopyIssueBean copyIssueBean, MutableIssue issueToCopy, List<String> visibleFieldIds) {
		ServiceOutcome<Collection<ApplicationUser>> collectionServiceOutcome = voteService
				.viewVoters(issueToCopy, authenticationContext.getLoggedInUser());
		if (collectionServiceOutcome.isValid()) {
			visibleFieldIds.add(IssueFieldConstants.VOTERS);
			Collection<ApplicationUser> returnedValue = collectionServiceOutcome.getReturnedValue();
			List<UserBean> voters = new ArrayList<UserBean>();
			for (ApplicationUser user : returnedValue) {
				voters.add(new UserBean(user.getName(), user.getEmailAddress(), user.getDisplayName()));
			}
			copyIssueBean.setVoters(voters);
		}
	}

	private boolean isTimeTrackingEnabled() {
		return timeTrackingConfiguration.enabled();
	}


}
