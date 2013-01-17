package com.atlassian.cpji.fields;

import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.custom.DateCFMapper;
import com.atlassian.cpji.fields.custom.GenericTextCFMapper;
import com.atlassian.cpji.fields.custom.LabelsCFMapper;
import com.atlassian.cpji.fields.custom.MultiGroupCFMapper;
import com.atlassian.cpji.fields.custom.MultiSelectListCFMapper;
import com.atlassian.cpji.fields.custom.MultiUserCFMapper;
import com.atlassian.cpji.fields.custom.NumberCFMapper;
import com.atlassian.cpji.fields.custom.ProjectCFMapper;
import com.atlassian.cpji.fields.custom.SelectListCFMapper;
import com.atlassian.cpji.fields.custom.UserCFMapper;
import com.atlassian.cpji.fields.custom.VersionCFMapper;
import com.atlassian.cpji.fields.system.AffectedVersionsFieldMapper;
import com.atlassian.cpji.fields.system.AssigneeFieldMapper;
import com.atlassian.cpji.fields.system.CommentFieldMapper;
import com.atlassian.cpji.fields.system.ComponentFieldMapper;
import com.atlassian.cpji.fields.system.DescriptionFieldMapper;
import com.atlassian.cpji.fields.system.DueDateFieldMapper;
import com.atlassian.cpji.fields.system.EnvironmentFieldMapper;
import com.atlassian.cpji.fields.system.FixVersionsFieldMapper;
import com.atlassian.cpji.fields.system.IssueSecurityFieldMapper;
import com.atlassian.cpji.fields.system.IssueTypeFieldMapper;
import com.atlassian.cpji.fields.system.LabelSystemFieldMapper;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.system.PriorityFieldMapper;
import com.atlassian.cpji.fields.system.ProjectSystemFieldMapper;
import com.atlassian.cpji.fields.system.ReporterFieldMapper;
import com.atlassian.cpji.fields.system.SummaryFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldIssueCreationFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.fields.system.TimeTrackingFieldMapper;
import com.atlassian.cpji.fields.system.VoterFieldMapper;
import com.atlassian.cpji.fields.system.WatcherFieldMapper;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.bc.issue.watcher.WatcherService;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.converters.DatePickerConverter;
import com.atlassian.jira.issue.customfields.converters.DateTimeConverter;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since v1.4
 */
public class FieldMapperFactory
{
	private Map<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper> orderableFieldMapper = new HashMap<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper>();
    private List<SystemFieldPostIssueCreationFieldMapper> postIssueCreationFieldMapper = new ArrayList<SystemFieldPostIssueCreationFieldMapper>();

	private final List<CustomFieldMapper> customFieldMappers = Lists.newArrayList();

    public FieldMapperFactory
            (
                    final ConstantsManager constantsManager,
                    final VersionManager versionManager,
                    final UserManager userManager,
                    final PermissionManager permissionManager,
                    final ApplicationProperties applicationProperties,
                    final ProjectComponentManager projectComponentManager,
                    final IssueSecuritySchemeManager issueSecuritySchemeManager,
                    final IssueSecurityLevelManager issueSecurityLevelManager,
                    final IssueTypeSchemeManager issueTypeSchemeManager,
                    final CommentService commentService,
                    final ProjectRoleManager projectRoleManager,
                    final GroupManager groupManager,
                    final JiraAuthenticationContext jiraAuthenticationContext,
                    final WatcherService watcherService,
                    final FieldManager fieldManager,
                    final VoteService voteService,
                    final DatePickerConverter datePickerConverter,
					final DateTimeConverter dateTimeConverter,
                    final DoubleConverter doubleConverter,
                    final UserMappingManager userMappingManager,
                    final TimeTrackingConfiguration timeTrackingConfiguration,
					final ProjectManager projectManager)
    {
		SystemFieldIssueCreationFieldMapper priorityFieldMapper = new PriorityFieldMapper(constantsManager, getOrderableField(fieldManager, IssueFieldConstants.PRIORITY));
        addFieldMapper(priorityFieldMapper.getField(), priorityFieldMapper);

        SystemFieldIssueCreationFieldMapper affectedVersionFieldMapper = new AffectedVersionsFieldMapper(versionManager, getOrderableField(fieldManager, IssueFieldConstants.AFFECTED_VERSIONS));
        addFieldMapper(affectedVersionFieldMapper.getField(), affectedVersionFieldMapper);

        SystemFieldIssueCreationFieldMapper assigneeFieldMapper = new AssigneeFieldMapper(permissionManager, applicationProperties, getOrderableField(fieldManager, IssueFieldConstants.ASSIGNEE), userMappingManager);
        addFieldMapper(assigneeFieldMapper.getField(), assigneeFieldMapper);

        SystemFieldIssueCreationFieldMapper componentFieldMapper = new ComponentFieldMapper(projectComponentManager, getOrderableField(fieldManager, IssueFieldConstants.COMPONENTS));
        addFieldMapper(componentFieldMapper.getField(), componentFieldMapper);

        SystemFieldIssueCreationFieldMapper descriptionFieldMapper = new DescriptionFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.DESCRIPTION));
        addFieldMapper(descriptionFieldMapper.getField(), descriptionFieldMapper);

        SystemFieldIssueCreationFieldMapper dueDateFieldMapper = new DueDateFieldMapper(permissionManager, getOrderableField(fieldManager, IssueFieldConstants.DUE_DATE));
        addFieldMapper(dueDateFieldMapper.getField(), dueDateFieldMapper);

        SystemFieldIssueCreationFieldMapper environmentFieldMapper = new EnvironmentFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.ENVIRONMENT));
        addFieldMapper(environmentFieldMapper.getField(), environmentFieldMapper);

        SystemFieldIssueCreationFieldMapper fixVersionsFieldMapper = new FixVersionsFieldMapper(versionManager, permissionManager, getOrderableField(fieldManager, IssueFieldConstants.FIX_FOR_VERSIONS));
        addFieldMapper(fixVersionsFieldMapper.getField(), fixVersionsFieldMapper);

        SystemFieldIssueCreationFieldMapper issueSecurityFieldMapper = new IssueSecurityFieldMapper(issueSecuritySchemeManager, issueSecurityLevelManager, permissionManager, getOrderableField(fieldManager, IssueFieldConstants.SECURITY));
        addFieldMapper(issueSecurityFieldMapper.getField(), issueSecurityFieldMapper);

        SystemFieldIssueCreationFieldMapper reporterFieldMapper = new ReporterFieldMapper(permissionManager, getOrderableField(fieldManager, IssueFieldConstants.REPORTER), userMappingManager);
        addFieldMapper(reporterFieldMapper.getField(), reporterFieldMapper);

        SystemFieldIssueCreationFieldMapper summaryFieldMapper = new SummaryFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.SUMMARY));
        addFieldMapper(summaryFieldMapper.getField(), summaryFieldMapper);

        SystemFieldIssueCreationFieldMapper issueTypeFieldMapper = new IssueTypeFieldMapper(issueTypeSchemeManager, getOrderableField(fieldManager, IssueFieldConstants.ISSUE_TYPE));
        addFieldMapper(issueTypeFieldMapper.getField(), issueTypeFieldMapper);

        SystemFieldIssueCreationFieldMapper projectSystemFieldMapper = new ProjectSystemFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.PROJECT));
        addFieldMapper(projectSystemFieldMapper.getField(), projectSystemFieldMapper);

        SystemFieldIssueCreationFieldMapper labelSysFieldMapper = new LabelSystemFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.LABELS));
        addFieldMapper(labelSysFieldMapper.getField(), labelSysFieldMapper);

        SystemFieldIssueCreationFieldMapper timeTrackinFieldMapper = new TimeTrackingFieldMapper(getOrderableField(fieldManager, IssueFieldConstants.TIMETRACKING), timeTrackingConfiguration);
        addFieldMapper(timeTrackinFieldMapper.getField(), timeTrackinFieldMapper);

        /**
         * SystemFieldPostIssueCreationFieldMapper
         */
        SystemFieldPostIssueCreationFieldMapper commentFieldMapper = new CommentFieldMapper(commentService, projectRoleManager, groupManager, permissionManager, getOrderableField(fieldManager, IssueFieldConstants.COMMENT), userMappingManager);
        postIssueCreationFieldMapper.add(commentFieldMapper);

        SystemFieldPostIssueCreationFieldMapper watcherFieldMapper = new WatcherFieldMapper(watcherService, permissionManager, jiraAuthenticationContext, createField(IssueFieldConstants.WATCHERS, "cpji.field.names.watchers"), userMappingManager);
        postIssueCreationFieldMapper.add(watcherFieldMapper);

        SystemFieldPostIssueCreationFieldMapper votersFieldMapper = new VoterFieldMapper(createField(IssueFieldConstants.VOTERS, "cpji.field.names.votes"), voteService, permissionManager, jiraAuthenticationContext, userMappingManager);
        postIssueCreationFieldMapper.add(votersFieldMapper);

        addCustomFieldMapper(new SelectListCFMapper());
		addCustomFieldMapper(new DateCFMapper(datePickerConverter, dateTimeConverter));
		addCustomFieldMapper(new NumberCFMapper(doubleConverter));
		addCustomFieldMapper(new GenericTextCFMapper());
		addCustomFieldMapper(new MultiGroupCFMapper(groupManager));
		addCustomFieldMapper(new ProjectCFMapper(projectManager));
		addCustomFieldMapper(new VersionCFMapper(versionManager));
		addCustomFieldMapper(new UserCFMapper(userManager));
		addCustomFieldMapper(new MultiUserCFMapper(userManager));
		addCustomFieldMapper(new LabelsCFMapper());
		addCustomFieldMapper(new MultiSelectListCFMapper());
    }

	private void addCustomFieldMapper(CustomFieldMapper mapper) {
		customFieldMappers.add(mapper);
	}

	private OrderableField getOrderableField(final FieldManager fieldManager, final String id)
    {
        Field field = fieldManager.getField(id);
        if (!(field instanceof OrderableField))
        {
            throw new RuntimeException("Field with id '" + id + "' is not an orderable field! Failed to initialize FieldMapper!");
        }
        return (OrderableField) field;
    }

    private Field createField(final String id, final String nameKey)
    {
        return new Field()
        {
            public String getId()
            {
                return id;
            }

            public String getNameKey()
            {
                return nameKey;
            }

            public String getName()
            {
                return "TODO";
            }

            public int compareTo(final Object o)
            {
                return 0;
            }
        };
    }

    public IssueCreationFieldMapper getIssueCreationFieldMapper(Class<? extends OrderableField> field)
    {
        IssueCreationFieldMapper fieldMapper = orderableFieldMapper.get(field);
        return fieldMapper;
    }

    private void addFieldMapper(Class<? extends OrderableField> field, SystemFieldIssueCreationFieldMapper fieldMapper)
    {
        if (orderableFieldMapper.containsKey(field))
        {
            throw new RuntimeException("Field mapper for field '" + field.getName() + "' already registered!");
        }
        orderableFieldMapper.put(field, fieldMapper);
    }

    public Map<String, FieldMapper> getSystemFieldMappers()
    {
        Map<String, FieldMapper> fieldMappers = new HashMap<String, FieldMapper>();
        for (SystemFieldIssueCreationFieldMapper fieldMapper : orderableFieldMapper.values())
        {
            fieldMappers.put(fieldMapper.getFieldId(), fieldMapper);
        }
        for (SystemFieldPostIssueCreationFieldMapper fieldMapper : postIssueCreationFieldMapper)
        {
            fieldMappers.put(fieldMapper.getFieldId(), fieldMapper);
        }
        return fieldMappers;
    }

    public List<SystemFieldPostIssueCreationFieldMapper> getPostIssueCreationFieldMapper()
    {
        return postIssueCreationFieldMapper;
    }

    public Map<String, NonOrderableSystemFieldMapper> getNonOrderableSystemFieldMappers()
    {
        Map<String, FieldMapper> systemFieldMappers = getSystemFieldMappers();
        Map<String, NonOrderableSystemFieldMapper> nonOrderableSystemFieldMappers = new HashMap<String, NonOrderableSystemFieldMapper>();
        for (FieldMapper fieldMapper : systemFieldMappers.values())
        {
            if (NonOrderableSystemFieldMapper.class.isAssignableFrom(fieldMapper.getClass()))
            {
               nonOrderableSystemFieldMappers.put(fieldMapper.getFieldId(), (NonOrderableSystemFieldMapper) fieldMapper);
            }
        }
        return nonOrderableSystemFieldMappers;
    }

    public CustomFieldMapper getCustomFieldMapper(CustomFieldType customFieldType)
    {
		for(CustomFieldMapper mapper : customFieldMappers) {
			if (mapper.acceptsType(customFieldType)) {
				return mapper;
			}
		}
        return null;
    }

}
