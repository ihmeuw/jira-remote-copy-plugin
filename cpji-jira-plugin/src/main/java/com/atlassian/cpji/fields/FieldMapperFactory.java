package com.atlassian.cpji.fields;

import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.system.CommentFieldMapper;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldIssueCreationFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.fields.system.VoterFieldMapper;
import com.atlassian.cpji.fields.system.WatcherFieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.bc.issue.vote.VoteService;
import com.atlassian.jira.bc.issue.watcher.WatcherService;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.util.concurrent.LazyReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.netbeans.lib.cvsclient.commandLine.command.add;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since v1.4
 */
public class FieldMapperFactory
{
	private final LazyReference<Map<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper>> orderableFieldMapper;

	private List<SystemFieldPostIssueCreationFieldMapper> postIssueCreationFieldMapper = new ArrayList<SystemFieldPostIssueCreationFieldMapper>();

	private final LazyReference<List<CustomFieldMapper>> customFieldMappers;

    public FieldMapperFactory
            (
					final PluginAccessor pluginAccessor,
                    final PermissionManager permissionManager,
                    final CommentService commentService,
                    final ProjectRoleManager projectRoleManager,
                    final GroupManager groupManager,
                    final JiraAuthenticationContext jiraAuthenticationContext,
                    final WatcherService watcherService,
                    final FieldManager fieldManager,
                    final VoteService voteService,
                    final UserMappingManager userMappingManager,
					final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		orderableFieldMapper = new LazyReference<Map<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper>>() {
			@Override
			protected Map<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper> create()
					throws Exception {
				final List<SystemFieldMapperModuleDescriptor> systemFieldsMappers = pluginAccessor.getEnabledModuleDescriptorsByClass(SystemFieldMapperModuleDescriptor.class);
				final Map<Class<? extends OrderableField>, SystemFieldIssueCreationFieldMapper> mappers = Maps.newHashMapWithExpectedSize(systemFieldsMappers.size());
				for (SystemFieldMapperModuleDescriptor mapper : systemFieldsMappers) {
					final Class<? extends OrderableField> field = mapper.getModule().getField();

					if (mappers.containsKey(field)) {
						throw new RuntimeException("Field mapper for field '" + field.getName() + "' already registered!");
					}
					mappers.put(field, mapper.getModule());
				}
				return mappers;
			}
		};

        /**
         * SystemFieldPostIssueCreationFieldMapper
         */
        SystemFieldPostIssueCreationFieldMapper commentFieldMapper = new CommentFieldMapper(commentService, projectRoleManager,
				groupManager, permissionManager, getOrderableField(fieldManager, IssueFieldConstants.COMMENT), userMappingManager, defaultFieldValuesManager);
		postIssueCreationFieldMapper.add(commentFieldMapper);

        SystemFieldPostIssueCreationFieldMapper watcherFieldMapper = new WatcherFieldMapper(watcherService, permissionManager,
				jiraAuthenticationContext, createField(IssueFieldConstants.WATCHERS, "cpji.field.names.watchers"), userMappingManager, defaultFieldValuesManager);
		postIssueCreationFieldMapper.add(watcherFieldMapper);

        SystemFieldPostIssueCreationFieldMapper votersFieldMapper = new VoterFieldMapper(createField(IssueFieldConstants.VOTERS, "cpji.field.names.votes"),
				voteService, permissionManager, jiraAuthenticationContext, userMappingManager, defaultFieldValuesManager);
		postIssueCreationFieldMapper.add(votersFieldMapper);

		customFieldMappers = new LazyReference<List<CustomFieldMapper>>() {
			@Override
			protected List<CustomFieldMapper> create() throws Exception {
				final List<CustomFieldMapper> mappers = Lists.newArrayList();
				final List<CustomFieldMapperModuleDescriptor> customFieldsMappers = pluginAccessor.getEnabledModuleDescriptorsByClass(CustomFieldMapperModuleDescriptor.class);
				for(CustomFieldMapperModuleDescriptor customFieldMapper : customFieldsMappers) {
					mappers.add(customFieldMapper.getModule());
				}
				return mappers;
			}
		};
    }

	public static OrderableField getOrderableField(final FieldManager fieldManager, final String id)
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
		IssueCreationFieldMapper fieldMapper = orderableFieldMapper.get().get(field);
        return fieldMapper;
    }

    public Map<String, FieldMapper> getSystemFieldMappers()
    {
        Map<String, FieldMapper> fieldMappers = new HashMap<String, FieldMapper>();
        for (SystemFieldIssueCreationFieldMapper fieldMapper : orderableFieldMapper.get().values())
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
		for(CustomFieldMapper mapper : customFieldMappers.get()) {
			if (mapper.acceptsType(customFieldType)) {
				return mapper;
			}
		}
        return null;
    }

}
