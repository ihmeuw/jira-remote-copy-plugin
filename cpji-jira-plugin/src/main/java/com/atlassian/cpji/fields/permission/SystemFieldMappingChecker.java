package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.SystemFieldMapper;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManagerImpl;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @since v1.4
 */
public class SystemFieldMappingChecker extends AbstractFieldMappingChecker<SystemFieldPermissionBean>
{
    private final FieldMapperFactory fieldMapperFactory;
    private final JiraAuthenticationContext authenticationContext;
	private final CachingUserMapper userMapper;

	private static final Logger log = Logger.getLogger(SystemFieldMappingChecker.class);

    public SystemFieldMappingChecker(final DefaultFieldValuesManagerImpl defaultFieldValuesManager,
			final FieldMapperFactory fieldMapperFactory, final JiraAuthenticationContext authenticationContext,
			final CopyIssueBean copyIssueBean, final Project project,
			FieldLayout fieldLayout, CachingUserMapper userMapper)
    {
        super(defaultFieldValuesManager, copyIssueBean, project, fieldLayout);
        this.fieldMapperFactory = fieldMapperFactory;
        this.authenticationContext = authenticationContext;
		this.userMapper = userMapper;
	}

    public List<SystemFieldPermissionBean> findUnmappedRemoteFields(CopyIssueBean copyIssueBean, Iterable<FieldLayoutItem> fieldLayoutItems)
    {
        final List<String> remoteFieldIds = copyIssueBean.getVisibleSystemFieldIds();
        final List<SystemFieldPermissionBean> fieldPermissionBeans = new ArrayList<SystemFieldPermissionBean>();
        for (String remoteFieldId : remoteFieldIds)
        {
            if (!containsSystemFieldWithId(remoteFieldId, fieldLayoutItems))
            {
                fieldPermissionBeans.add(new SystemFieldPermissionBean(remoteFieldId, ValidationCode.FIELD_NOT_MAPPED.name(), Collections.<String>emptyList()));
            }
        }
        return fieldPermissionBeans;
    }

    public SystemFieldPermissionBean getFieldPermission(final String fieldId)
    {
        final Map<String, SystemFieldMapper> fieldMappers = fieldMapperFactory.getSystemFieldMappers();
        final List<String> remoteFieldIds = copyIssueBean.getVisibleSystemFieldIds();

        if (!remoteFieldIds.contains(fieldId))
        {
            if (isFieldRequired(fieldLayout, fieldId))
            {
                if (hasDefaultValue(fieldId))
                {
                    return new SystemFieldPermissionBean(fieldId, ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE.name(), Collections.<String>emptyList());
                }
                return new SystemFieldPermissionBean(fieldId, ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED.name(), Collections.<String>emptyList());
            }
            else
            {
                 return null;
            }
        }

		final SystemFieldMapper systemFieldMapper = fieldMappers.get(fieldId);
        if (systemFieldMapper != null)
        {
            final boolean hasPermission = systemFieldMapper.userHasRequiredPermission(project, authenticationContext.getLoggedInUser());
            final MappingResult mappingResult = systemFieldMapper.getMappingResult(userMapper, copyIssueBean, project);
            PermissionBeanCreator<SystemFieldPermissionBean> permissionBeanCreator = new PermissionBeanCreator<SystemFieldPermissionBean>()
            {
                public SystemFieldPermissionBean createPermissionBean(final ValidationCode validationCode)
                {
                    return new SystemFieldPermissionBean(fieldId, validationCode.name(), mappingResult.getUnmappedValues());
                }
            };
            return checkField(mappingResult, fieldId, hasPermission, permissionBeanCreator);
        }
        else
        {
            log.warn("No support yet for system field '" + fieldId + "'");
            return null;
        }
    }

    private boolean containsSystemFieldWithId(final String id, Iterable<FieldLayoutItem> layoutItems)
    {
        try
        {
            FieldLayoutItem fieldLayoutItem = Iterables.find(layoutItems, new Predicate<FieldLayoutItem>()
            {
                public boolean apply(final FieldLayoutItem input)
                {
                    return id.equals(input.getOrderableField().getId());
                }
            });
            return fieldLayoutItem != null;
        }
        catch (NoSuchElementException ex)
        {
            NonOrderableSystemFieldMapper nonOrderableSystemFieldMapper = fieldMapperFactory.getNonOrderableSystemFieldMappers().get(id);
            if (nonOrderableSystemFieldMapper != null)
            {
                return nonOrderableSystemFieldMapper.isVisible();
            }
            return false;
        }
    }

}
