package com.atlassian.cpji.rest;

import com.atlassian.cpji.fields.CustomFieldMappingResult;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.permission.CustomFieldMapperUtil;
import com.atlassian.cpji.fields.permission.CustomFieldMappingChecker;
import com.atlassian.cpji.fields.permission.SystemFieldMappingChecker;
import com.atlassian.cpji.fields.system.FieldCreationException;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.cpji.rest.model.CustomFieldPermissionBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.cpji.rest.model.RemoteUserBean;
import com.atlassian.cpji.rest.model.SystemFieldPermissionBean;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.ProjectSystemField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutStorageException;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * @since v1.0
 */
@Path ("copyissue")
@Consumes ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class CopyIssueResource
{
    private final IssueService issueService;
    private final JiraAuthenticationContext authenticationContext;
    private final PermissionManager permissionManager;
    private final ProjectService projectService;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final ApplicationProperties applicationProperties;
    private final FieldMapperFactory fieldMapperFactory;
    private final FieldManager fieldManager;
    private final DefaultFieldValuesManager defaultFieldValuesManager;
    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final BuildUtilsInfo buildUtilsInfo;

    private static final Logger log = Logger.getLogger(CopyIssueResource.class);

    public CopyIssueResource
            (
                    final IssueService issueService,
                    final JiraAuthenticationContext authenticationContext,
                    final PermissionManager permissionManager,
                    final ProjectService projectService,
                    final IssueTypeSchemeManager issueTypeSchemeManager,
                    final FieldLayoutManager fieldLayoutManager,
                    final ApplicationProperties applicationProperties,
                    final FieldMapperFactory fieldMapperFactory,
                    final FieldManager fieldManager,
                    final DefaultFieldValuesManager defaultFieldValuesManager,
                    final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
                    final BuildUtilsInfo buildUtilsInfo)
    {
        this.issueService = issueService;
        this.authenticationContext = authenticationContext;
        this.permissionManager = permissionManager;
        this.projectService = projectService;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.applicationProperties = applicationProperties;
        this.fieldMapperFactory = fieldMapperFactory;
        this.fieldManager = fieldManager;
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.buildUtilsInfo = buildUtilsInfo;
    }


    @PUT
    @Path ("copy")
    public Response copyIssue(final CopyIssueBean copyIssueBean) throws FieldLayoutStorageException
    {
        ProjectService.GetProjectResult result = projectService.getProjectByKey(authenticationContext.getLoggedInUser(), copyIssueBean.getTargetProjectKey());
        Project project;
        if (result.isValid())
        {
            project = result.getProject();
        }
        else
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(result.getErrorCollection())).cacheControl(RESTException.never()).build();
        }

        final IssueType issueType = findIssueType(copyIssueBean.getTargetIssueType(), project);
        Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType);
        //Not let's start copying values over from the original issue.
        IssueInputParametersImpl inputParameters = new IssueInputParametersImpl();
        IssueCreationFieldMapper projectFieldMapper = fieldMapperFactory.getIssueCreationFieldMapper(ProjectSystemField.class);
        projectFieldMapper.populateInputParameters(inputParameters, copyIssueBean, null, project);
        Map<String,FieldMapper> allSystemFieldMappers = fieldMapperFactory.getSystemFieldMappers();
        for (FieldLayoutItem fieldLayoutItem : fieldLayoutItems)
        {
            OrderableField orderableField = fieldLayoutItem.getOrderableField();
            if (!fieldManager.isCustomField(orderableField))
            {
                IssueCreationFieldMapper fieldMapper = fieldMapperFactory.getIssueCreationFieldMapper(orderableField.getClass());
                if (fieldMapper != null)
                {
                    MappingResult mappingResult = fieldMapper.getMappingResult(copyIssueBean, project);
                    if (!mappingResult.hasOneValidValue() && fieldLayoutItem.isRequired())
                    {
                        String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), orderableField.getId(), issueType.getName());
                        if (defaultFieldValue != null)
                        {
                            inputParameters.getActionParameters().put(orderableField.getId(), defaultFieldValue);
                        }
                    }
                    else
                    {
                        fieldMapper.populateInputParameters(inputParameters, copyIssueBean, fieldLayoutItem, project);
                    }
                }
                else
                {
                    if (!allSystemFieldMappers.containsKey(orderableField.getId()))
                    {
                        log.warn("No support for field '" + orderableField.getName() + "'");
                    }
                }
            }
            else
            {
                CustomField customField = fieldManager.getCustomField(orderableField.getId());
                CustomFieldMapper customFieldMapper = fieldMapperFactory.getCustomFieldMapper().get(customField.getCustomFieldType().getClass().getCanonicalName());
                if (customFieldMapper != null)
                {
                    CustomFieldBean matchingRemoteCustomField = CustomFieldMapperUtil.findMatchingRemoteCustomField(customField, copyIssueBean.getCustomFields());
                    if (matchingRemoteCustomField != null)
                    {
                        CustomFieldMappingResult customFieldMappingResult = customFieldMapper.getMappingResult(matchingRemoteCustomField, customField, project, issueType);
                        if (!customFieldMappingResult.hasOneValidValue() && fieldLayoutItem.isRequired())
                        {
                            String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), orderableField.getId(), issueType.getName());
                            if (defaultFieldValue != null)
                            {
                                inputParameters.addCustomFieldValue(orderableField.getId(), defaultFieldValue);
                            }
                        }
                        else
                        {
                            customFieldMapper.populateInputParameters(inputParameters, customFieldMappingResult, customField, project, issueType);
                        }
                    }
                    else if (fieldLayoutItem.isRequired())
                    {
                        String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), orderableField.getId(), issueType.getName());
                        if (defaultFieldValue != null)
                        {
                            inputParameters.addCustomFieldValue(orderableField.getId(), defaultFieldValue);
                        }
                    }
                }
                else
                {
                   log.warn("No support yet for custom field type '" + customField.getCustomFieldType().getClass().getCanonicalName() + "'");
                }
            }
        }

        IssueService.CreateValidationResult validationResult = issueService.validateCreate(authenticationContext.getLoggedInUser(), inputParameters);

        if (!validationResult.isValid())
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(validationResult.getErrorCollection())).cacheControl(RESTException.never()).build();
        }

        IssueService.IssueResult createIssueResult = issueService.create(authenticationContext.getLoggedInUser(), validationResult);

        if (createIssueResult.isValid())
        {
            final List<SystemFieldPostIssueCreationFieldMapper> postIssueCreationFieldMapper = fieldMapperFactory.getPostIssueCreationFieldMapper();
            final List<String> errors = new ArrayList<String>();
            for (SystemFieldPostIssueCreationFieldMapper issueCreationFieldMapper : postIssueCreationFieldMapper)
            {
                try
                {
                    issueCreationFieldMapper.process(createIssueResult.getIssue(), copyIssueBean);
                }
                catch (FieldCreationException e)
                {
                    log.warn("Exception when creating field '" + e.getFieldId() + "'", e);
                    errors.add(e.getMessage());
                }
            }
            IssueCreationResultBean resultBean = new IssueCreationResultBean(createIssueResult.getIssue().getKey(), createIssueResult.getIssue().getProjectObject().getKey(), createIssueResult.getIssue().getId());
            if (!errors.isEmpty())
            {
                errors.add(0, "Created issue '" + createIssueResult.getIssue().getKey() + "'. But there were some errors.'");
                return Response.serverError().entity(new ErrorBean(errors)).cacheControl(RESTException.never()).build();
            }
            return Response.ok(resultBean).cacheControl(RESTException.never()).build();
        }
        else
        {
            log.error("Failed to create issue. Reason: " + createIssueResult.getErrorCollection());
            return Response.serverError().entity(ErrorBean.convertErrorCollection(createIssueResult.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
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


    @GET
    @Path ("issueTypeInformation/{project}")
    public Response getIssueTypeInformation(@PathParam ("project") String projectKey)
    {
        ProjectService.GetProjectResult result = projectService.getProjectByKey(authenticationContext.getLoggedInUser(), projectKey);
        Project project;
        if (result.isValid())
        {
            project = result.getProject();
        }
        else
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(result.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
        RemoteUserBean userBean = new RemoteUserBean(authenticationContext.getLoggedInUser().getName(), authenticationContext.getLoggedInUser().getDisplayName());
        boolean hasCreateIssuePermission = permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, authenticationContext.getLoggedInUser());
        boolean hasCreateAttachmentPermission = permissionManager.hasPermission(Permissions.CREATE_ATTACHMENT, project, authenticationContext.getLoggedInUser());

        if (hasCreateIssuePermission)
        {
            Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
            List<String> issueTypes = new ArrayList<String>();
            for (IssueType issueType : issueTypesForProject)
            {
                issueTypes.add(issueType.getName());
            }
            IssueTypeBean issueTypesBean = new IssueTypeBean(issueTypes);
            boolean attachmentsDisabled = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
            CopyInformationBean copyInformationBean = new CopyInformationBean(issueTypesBean, attachmentsDisabled, userBean, hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return Response.ok(copyInformationBean).cacheControl(RESTException.never()).build();
        }
        else
        {
            CopyInformationBean copyInformationBean = new CopyInformationBean(null, true, userBean, hasCreateIssuePermission, hasCreateAttachmentPermission, buildUtilsInfo.getVersion());
            return Response.ok(copyInformationBean).cacheControl(RESTException.never()).build();
        }
    }

    @PUT
    @Path ("fieldPermissions")
    public Response checkFieldPermissions(final CopyIssueBean copyIssueBean)
            throws FieldLayoutStorageException
    {
        ProjectService.GetProjectResult result = projectService.getProjectByKey(authenticationContext.getLoggedInUser(), copyIssueBean.getTargetProjectKey());
        Project project;
        if (result.isValid())
        {
            project = result.getProject();
        }
        else
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(result.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
        final IssueType issueType = findIssueType(copyIssueBean.getTargetIssueType(), project);
        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());

        Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType);

        final List<SystemFieldPermissionBean> systemFieldPermissionBeans = new ArrayList<SystemFieldPermissionBean>();
        final List<CustomFieldPermissionBean> customFieldPermissionBeans = new ArrayList<CustomFieldPermissionBean>();
        SystemFieldMappingChecker systemFieldMappingChecker = new SystemFieldMappingChecker(defaultFieldValuesManager, fieldMapperFactory, authenticationContext, copyIssueBean, project, fieldLayout);
        CustomFieldMappingChecker customFieldMappingChecker = new CustomFieldMappingChecker(defaultFieldValuesManager, copyIssueBean, project, fieldLayout, fieldManager, fieldMapperFactory, issueTypeSchemeManager);

        systemFieldPermissionBeans.addAll(systemFieldMappingChecker.findUnmappedRemoteFields(copyIssueBean, fieldLayoutItems));
        customFieldPermissionBeans.addAll(customFieldMappingChecker.findUnmappedRemoteFields(copyIssueBean, fieldLayoutItems));

        Iterable<String> orderableFieldIds = Iterables.transform(fieldLayoutItems, new Function<FieldLayoutItem, String>()
        {
            public String apply(final FieldLayoutItem from)
            {
                return from.getOrderableField().getId();
            }
        });
        ArrayList<String> fieldIds = Lists.newArrayList(orderableFieldIds);
        Map<String,NonOrderableSystemFieldMapper> nonOrderableSystemFieldMappers = fieldMapperFactory.getNonOrderableSystemFieldMappers();
        for (NonOrderableSystemFieldMapper nonOrderableSystemFieldMapper : nonOrderableSystemFieldMappers.values())
        {
            if (nonOrderableSystemFieldMapper.isVisible())
            {
                fieldIds.add(nonOrderableSystemFieldMapper.getFieldId());
            }
        }

        for (String fieldId : fieldIds)
        {
            if (fieldManager.isCustomField(fieldId))
            {
                CustomFieldPermissionBean customFieldPermissionBean = customFieldMappingChecker.getFieldPermission(fieldId);
                if (customFieldPermissionBean != null)
                {
                    customFieldPermissionBeans.add(customFieldPermissionBean);
                }
            }
            else
            {
                SystemFieldPermissionBean fieldPermission = systemFieldMappingChecker.getFieldPermission(fieldId);
                if (fieldPermission != null)
                {
                    systemFieldPermissionBeans.add(fieldPermission);
                }
            }
        }

        return Response.ok(new FieldPermissionsBean(systemFieldPermissionBeans, customFieldPermissionBeans)).cacheControl(RESTException.never()).build();
    }

}
