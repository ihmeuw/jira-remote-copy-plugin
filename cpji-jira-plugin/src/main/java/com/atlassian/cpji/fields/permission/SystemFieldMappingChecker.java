package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class SystemFieldMappingChecker extends AbstractFieldMappingChecker<SystemFieldPermissionBean>
{
    private final FieldMapperFactory fieldMapperFactory;
    private final JiraAuthenticationContext authenticationContext;

    private static final Logger log = Logger.getLogger(SystemFieldMappingChecker.class);

    public SystemFieldMappingChecker(final DefaultFieldValuesManager defaultFieldValuesManager, final FieldMapperFactory fieldMapperFactory, final JiraAuthenticationContext authenticationContext, final CopyIssueBean copyIssueBean, final Project project, FieldLayout fieldLayout)
    {
        super(defaultFieldValuesManager, copyIssueBean, project, fieldLayout);
        this.fieldMapperFactory = fieldMapperFactory;
        this.authenticationContext = authenticationContext;
    }

    public List<SystemFieldPermissionBean> findUnmappedRemoteFields(CopyIssueBean copyIssueBean, Iterable<FieldLayoutItem> fieldLayoutItems)
    {
        final List<String> remoteFieldIds = copyIssueBean.getVisibleSystemFieldIds();
        final List<SystemFieldPermissionBean> fieldPermissionBeans = new ArrayList<SystemFieldPermissionBean>();
        for (String remoteFieldId : remoteFieldIds)
        {
            if (!containsSystemFieldWithId(remoteFieldId, fieldLayoutItems))
            {
                fieldPermissionBeans.add(new SystemFieldPermissionBean(remoteFieldId, ValidationCode.FIELD_NOT_MAPPED.name()));
            }
        }
        return fieldPermissionBeans;
    }

    public SystemFieldPermissionBean getFieldPermission(final String fieldId)
    {
        final Map<String, FieldMapper> fieldMappers = fieldMapperFactory.getSystemFieldMappers();
        final List<String> remoteFieldIds = copyIssueBean.getVisibleSystemFieldIds();

        if (!remoteFieldIds.contains(fieldId))
        {
            if (isFieldRequired(fieldLayout, fieldId))
            {
                if (defaultValueConfigured(fieldId))
                {
                    return new SystemFieldPermissionBean(fieldId, ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE.name());
                }
                return new SystemFieldPermissionBean(fieldId, ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED.name());
            }
            else
            {
                 return null;
            }
        }

		final FieldMapper fieldMapper = fieldMappers.get(fieldId);
        if (fieldMapper != null)
        {
            final boolean hasPermission = fieldMapper.userHasRequiredPermission(project, authenticationContext.getLoggedInUser());
            final MappingResult mappingResult = fieldMapper.getMappingResult(copyIssueBean, project);
            PermissionBeanCreator<SystemFieldPermissionBean> permissionBeanCreator = new PermissionBeanCreator<SystemFieldPermissionBean>()
            {
                public SystemFieldPermissionBean createPermissionBean(final ValidationCode validationCode)
                {
                    return new SystemFieldPermissionBean(fieldId, validationCode.name());
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
