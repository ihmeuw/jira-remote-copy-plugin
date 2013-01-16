package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.ValidationCode;
import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.RESTException;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.jira.issue.customfields.impl.ReadOnlyCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class CustomFieldMappingChecker extends AbstractFieldMappingChecker<CustomFieldPermissionBean>
{
    private final FieldManager fieldManager;
    private final FieldMapperFactory fieldMapperFactory;
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    private static final Logger log = Logger.getLogger(CustomFieldMappingChecker.class);

    public CustomFieldMappingChecker(final DefaultFieldValuesManager defaultFieldValuesManager, final CopyIssueBean copyIssueBean, final Project project, FieldLayout fieldLayout, final FieldManager fieldManager, final FieldMapperFactory fieldMapperFactory, final IssueTypeSchemeManager issueTypeSchemeManager)
    {
        super(defaultFieldValuesManager, copyIssueBean, project, fieldLayout);
        this.fieldManager = fieldManager;
        this.fieldMapperFactory = fieldMapperFactory;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
    }


    public List<CustomFieldPermissionBean> findUnmappedRemoteFields(CopyIssueBean copyIssueBean, Iterable<FieldLayoutItem> fieldLayoutItems)
    {
        final List<CustomFieldPermissionBean> fieldPermissionBeans = new ArrayList<CustomFieldPermissionBean>();
        List<CustomFieldBean> remoteCustomFields = copyIssueBean.getCustomFields();
        //Check custom fields
        if (remoteCustomFields != null)
        {
            for (CustomFieldBean customField : remoteCustomFields)
            {
                if (!containsCustomFieldWithNameAndType(customField, fieldLayoutItems))
                {
                    fieldPermissionBeans.add(new CustomFieldPermissionBean(
							customField.getId(),
							customField.getCustomFieldName(),
							ValidationCode.FIELD_NOT_MAPPED.name()));
                }
            }
        }
        return fieldPermissionBeans;
    }

    private boolean containsCustomFieldWithNameAndType(final CustomFieldBean customFieldBean, Iterable<FieldLayoutItem> layoutItems)
    {
        try
        {
            FieldLayoutItem fieldLayoutItem = Iterables.find(layoutItems, new Predicate<FieldLayoutItem>()
            {
                public boolean apply(final FieldLayoutItem input)
                {
                    if (fieldManager.isCustomField(input.getOrderableField()))
                    {
                        CustomField customField = fieldManager.getCustomField(input.getOrderableField().getId());
                        if (CustomFieldMapperUtil.customFieldMatchesTypeAndName(customField, customFieldBean.getCustomFieldType(), customFieldBean.getCustomFieldName()))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            });
            return fieldLayoutItem != null;
        }
        catch (NoSuchElementException ex)
        {
            return false;
        }
    }

    public CustomFieldPermissionBean getFieldPermission(String fieldId)
    {
        final CustomField customField = fieldManager.getCustomField(fieldId);
        if (customField == null)
        {
            throw new RuntimeException("Field with id '" + fieldId + "' is not a custom field!");
        }
        CustomFieldBean matchingRemoteCustomField = CustomFieldMapperUtil.findMatchingRemoteCustomField(customField, copyIssueBean.getCustomFields());
        if (matchingRemoteCustomField == null)
        {
            if (isFieldRequired(fieldLayout, fieldId) && !(customField.getCustomFieldType() instanceof ReadOnlyCFType))
            {
                if (defaultValueConfigured(fieldId))
                {
                    return new CustomFieldPermissionBean(
							customField.getId(),
							customField.getName(),
							ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED_USING_DEFAULT_VALUE.name());
                }
                return new CustomFieldPermissionBean(
						customField.getId(),
						customField.getName(),
						ValidationCode.FIELD_MANDATORY_BUT_NOT_SUPPLIED.name());
            }
            else
            {
                return null;
            }
        }

        CustomFieldMapper fieldMapper = fieldMapperFactory.getCustomFieldMapper(customField.getCustomFieldType());
        if (fieldMapper == null)
        {
            log.info("No mapper for custom field '" + customField.getCustomFieldType().getClass().getCanonicalName() + "' found.");
            return null;
        }

        final MappingResult mappingResult = fieldMapper.getMappingResult(matchingRemoteCustomField, customField, project, findIssueType(copyIssueBean.getTargetIssueType(), project));
        PermissionBeanCreator<CustomFieldPermissionBean> beanCreator = new PermissionBeanCreator<CustomFieldPermissionBean>()
        {
            public CustomFieldPermissionBean createPermissionBean(final ValidationCode validationCode)
            {
                return new CustomFieldPermissionBean(
						customField.getId(),
						customField.getName(),
						validationCode.name());
            }
        };
        return checkField(mappingResult, fieldId, true, beanCreator);
    }



    private IssueType findIssueType(final String issueType, final Project project)
    {
        Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
        try
        {
            return Iterables.find(issueTypesForProject, new Predicate<IssueType>()
            {
                public boolean apply(final IssueType input)
                {
                    return input.getName().equals(issueType);
                }
            });
        }
        catch (NoSuchElementException ex)
        {
            throw new RESTException(Response.Status.NOT_FOUND, "No issue type with name '" + issueType + "' found!");
        }
    }
}

