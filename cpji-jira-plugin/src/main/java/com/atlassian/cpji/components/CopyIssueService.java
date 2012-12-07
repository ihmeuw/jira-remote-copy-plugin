package com.atlassian.cpji.components;

import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.exceptions.*;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.permission.CustomFieldMappingChecker;
import com.atlassian.cpji.fields.permission.SystemFieldMappingChecker;
import com.atlassian.cpji.fields.system.FieldCreationException;
import com.atlassian.cpji.fields.system.NonOrderableSystemFieldMapper;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.rest.RESTException;
import com.atlassian.cpji.rest.model.*;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * @since v3.0
 */
public class CopyIssueService {
    private static final List<String> GLOBAL_ID_KEYS = ImmutableList.of("appId", "issueId");
    private static final Logger log = Logger.getLogger(CopyIssueService.class);

    private final IssueService issueService;
    private final JiraAuthenticationContext authenticationContext;
    private final ProjectService projectService;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final FieldMapperFactory fieldMapperFactory;
    private final FieldManager fieldManager;
    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final InternalHostApplication internalHostApplication;
    private final IssueLinkService issueLinkService;
    private final RemoteIssueLinkService remoteIssueLinkService;
    private final InputParametersService inputParametersService;

    public CopyIssueService(final IssueService issueService, final JiraAuthenticationContext authenticationContext, final ProjectService projectService, final IssueTypeSchemeManager issueTypeSchemeManager, final FieldLayoutManager fieldLayoutManager, final FieldMapperFactory fieldMapperFactory, final FieldManager fieldManager, final FieldLayoutItemsRetriever fieldLayoutItemsRetriever, final InternalHostApplication internalHostApplication, final IssueLinkService issueLinkService, final RemoteIssueLinkService remoteIssueLinkService, InputParametersService inputParametersService) {
        this.issueService = issueService;
        this.authenticationContext = authenticationContext;
        this.projectService = projectService;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.fieldMapperFactory = fieldMapperFactory;
        this.fieldManager = fieldManager;
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.internalHostApplication = internalHostApplication;
        this.issueLinkService = issueLinkService;
        this.remoteIssueLinkService = remoteIssueLinkService;
        this.inputParametersService = inputParametersService;
    }


    public IssueCreationResultBean copyIssue(final CopyIssueBean copyIssueBean)
            throws ValidationException, IssueCreatedWithErrorsException, CreationException, ProjectNotFoundException {
        Project project = getProjectFromIssueBean(copyIssueBean);

        final IssueType issueType = findIssueType(copyIssueBean.getTargetIssueType(), project);


        //Not let's start copying values over from the original issue.
        Map<String, FieldMapper> allSystemFieldMappers = fieldMapperFactory.getSystemFieldMappers();
        InputParametersService.Populator builder = inputParametersService.getFieldsPopulator(project, issueType, copyIssueBean, allSystemFieldMappers);

        builder.populateProjectSystemField();

        Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType);
        for (FieldLayoutItem fieldLayoutItem : fieldLayoutItems) {
            builder.injectInputParam(fieldLayoutItem);
        }

        IssueInputParameters inputParameters = builder.getInputParameters();

        IssueService.CreateValidationResult validationResult = issueService.validateCreate(callingUser(), inputParameters);

        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult.getErrorCollection());
        }

        IssueService.IssueResult createIssueResult = issueService.create(callingUser(), validationResult);

        if (createIssueResult.isValid()) {
            final List<SystemFieldPostIssueCreationFieldMapper> postIssueCreationFieldMapper = fieldMapperFactory.getPostIssueCreationFieldMapper();
            final ErrorCollection errors = new SimpleErrorCollection();
            for (SystemFieldPostIssueCreationFieldMapper issueCreationFieldMapper : postIssueCreationFieldMapper) {
                try {
                    issueCreationFieldMapper.process(createIssueResult.getIssue(), copyIssueBean);
                } catch (FieldCreationException e) {
                    log.warn("Exception when creating field '" + e.getFieldId() + "'", e);
                    errors.addError(issueCreationFieldMapper.getFieldId(), e.getMessage());
                }
            }

            IssueCreationResultBean resultBean = new IssueCreationResultBean(createIssueResult.getIssue().getKey(), createIssueResult.getIssue().getProjectObject().getKey(), createIssueResult.getIssue().getId());

            if (errors.hasAnyErrors()) {
                errors.addErrorMessage("Created issue '" + createIssueResult.getIssue().getKey() + "'. But there were some errors.'");
                throw new IssueCreatedWithErrorsException(resultBean, errors);
            }

            return resultBean;
        } else {
            log.error("Failed to create issue. Reason: " + createIssueResult.getErrorCollection());
            throw new CreationException(createIssueResult.getErrorCollection());
        }

    }

    private static class OnlyVisible implements Predicate<NonOrderableSystemFieldMapper>{
        @Override
        public boolean apply(@Nullable NonOrderableSystemFieldMapper input) {
            return input.isVisible();
        }
    }

    private static class GetFieldIdFromLayoutItem implements Function<FieldLayoutItem, String>{
        @Override
        public String apply(@Nullable FieldLayoutItem input) {
            return input.getOrderableField().getId();
        }
    }

    private static class GetIdFromNonOrderableMapper implements Function<NonOrderableSystemFieldMapper, String>{

        @Override
        public String apply(@Nullable NonOrderableSystemFieldMapper input) {
            return input.getFieldId();
        }
    }


    public FieldPermissionsBean checkFieldPermissions(final CopyIssueBean copyIssueBean) throws ProjectNotFoundException {
        Project project = getProjectFromIssueBean(copyIssueBean);
        final IssueType issueType = findIssueType(copyIssueBean.getTargetIssueType(), project);

        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());

        Iterable<FieldLayoutItem> fieldLayoutItems = fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType);

        final List<SystemFieldPermissionBean> systemFieldPermissionBeans = new ArrayList<SystemFieldPermissionBean>();
        final List<CustomFieldPermissionBean> customFieldPermissionBeans = new ArrayList<CustomFieldPermissionBean>();
        SystemFieldMappingChecker systemFieldMappingChecker = inputParametersService.getSystemFieldMappingChecker(project, copyIssueBean, fieldLayout);
        CustomFieldMappingChecker customFieldMappingChecker = inputParametersService.getCustomFieldMappingChecker(project, copyIssueBean, fieldLayout);

        systemFieldPermissionBeans.addAll(systemFieldMappingChecker.findUnmappedRemoteFields(copyIssueBean, fieldLayoutItems));
        customFieldPermissionBeans.addAll(customFieldMappingChecker.findUnmappedRemoteFields(copyIssueBean, fieldLayoutItems));


        Iterable<String> orderableFieldIds = Iterables.transform(fieldLayoutItems, new GetFieldIdFromLayoutItem());

        Collection<NonOrderableSystemFieldMapper> nonOrderableSystemFieldMappers = fieldMapperFactory.getNonOrderableSystemFieldMappers().values();
        Iterable<String> nonOrderableFieldIds = Iterables.transform(
        Iterables.filter(nonOrderableSystemFieldMappers, new OnlyVisible()), new GetIdFromNonOrderableMapper());

        Iterable<String> fieldIds = Iterables.concat(orderableFieldIds, nonOrderableFieldIds);

        for (String fieldId : fieldIds) {
            if (fieldManager.isCustomField(fieldId)) {
                CustomFieldPermissionBean customFieldPermissionBean = customFieldMappingChecker.getFieldPermission(fieldId);
                if (customFieldPermissionBean != null) {
                    customFieldPermissionBeans.add(customFieldPermissionBean);
                }
            } else {
                SystemFieldPermissionBean fieldPermission = systemFieldMappingChecker.getFieldPermission(fieldId);
                if (fieldPermission != null) {
                    systemFieldPermissionBeans.add(fieldPermission);
                }
            }
        }

        return new FieldPermissionsBean(systemFieldPermissionBeans, customFieldPermissionBeans);
    }

    public void convertRemoteLinksToLocal(String issueKey) throws IssueNotFoundException, RemoteLinksNotFoundException, IssueLinkCreationException {
        final User user = callingUser();

        // Get issue
        final IssueService.IssueResult result = issueService.getIssue(user, issueKey);

        if (!result.isValid()) {
            throw new IssueNotFoundException(result.getErrorCollection());
        }
        final Issue issue = result.getIssue();

        // Get remote issue links
        final RemoteIssueLinkService.RemoteIssueLinkListResult linksResult = remoteIssueLinkService.getRemoteIssueLinksForIssue(user, issue);
        if (!linksResult.isValid()) {
            throw new RemoteLinksNotFoundException(linksResult.getErrorCollection());
        }

        SimpleErrorCollection errors = new SimpleErrorCollection();

        for (final RemoteIssueLink remoteIssueLink : linksResult.getRemoteIssueLinks()) {
            // We are only interested in JIRA links
            if (RemoteIssueLink.APPLICATION_TYPE_JIRA.equals(remoteIssueLink.getApplicationType())) {
                final Map<String, String> values = decode(remoteIssueLink.getGlobalId(), GLOBAL_ID_KEYS);
                if (internalHostApplication.getId().get().equals(values.get("appId"))) {
                    // It links to this JIRA instance, make it a local link
                    final Issue issueToLinkTo = getIssue(user, Long.parseLong(values.get("issueId")));
                    try{
                        createIssueLink(user, issue, issueToLinkTo, remoteIssueLink.getRelationship());
                        // Delete the remote issue link, it is no longer needed
                        final RemoteIssueLinkService.DeleteValidationResult deleteValidationResult = remoteIssueLinkService.validateDelete(user, remoteIssueLink.getId());
                        if (deleteValidationResult.isValid()) {
                            remoteIssueLinkService.delete(user, deleteValidationResult);
                        }
                    } catch(IssueLinkCreationException e){
                        errors.addError(remoteIssueLink.getGlobalId(), e.toString());
                    }
                }
            }
        }

        if(errors.hasAnyErrors()){
            throw new IssueLinkCreationException(errors);
        }
    }


    private void createIssueLink(final User user, final Issue fromIssue, final Issue toIssue, final String relationship) throws IssueLinkCreationException {
        final IssueLinkService.AddIssueLinkValidationResult addIssueLinkValidationResult = issueLinkService.validateAddIssueLinks(
                user, fromIssue, relationship, ImmutableList.<String>of(toIssue.getKey()));

        if (addIssueLinkValidationResult.isValid()) {
            issueLinkService.addIssueLinks(user, addIssueLinkValidationResult);
        } else {
            log.warn("Error creating local link from " + fromIssue.getKey() + " to " + toIssue.getKey() + ". " + addIssueLinkValidationResult.getErrorCollection());
            throw new IssueLinkCreationException(addIssueLinkValidationResult.getErrorCollection());
        }
    }

    private Issue getIssue(final User user, final Long issueId) {
        final IssueService.IssueResult result = issueService.getIssue(user, issueId);
        if (!result.isValid()) {
            return null;
        }

        return result.getIssue();
    }

    // TODO, once a newer version of 5.0 is released (e.g. beta3), change to use GlobalIdFactory in the view-issue-plugin

    /**
     * Decode the given String to a Map of values.
     *
     * @param globalId the String to decode
     * @param keys     the order in which the keys should appear. If the keys are not in this order an
     *                 IllegalArgumentException is thrown.
     * @return a Map of values
     */
    public static Map<String, String> decode(final String globalId, final List<String> keys) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        final Scanner scanner = new Scanner(globalId);

        try {
            URLEncodedUtils.parse(params, scanner, "UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException("globalId is invalid, expected format is: " + getExpectedFormat(keys) + ", found: " + globalId, e);
        }

        // Check that we have the right number of keys
        if (params.size() != keys.size()) {
            throw new IllegalArgumentException("globalId is invalid, expected format is: " + getExpectedFormat(keys) + ", found: " + globalId);
        }

        // Get the values, and make sure the keys are in the correct order
        final Map<String, String> result = new HashMap<String, String>(params.size());
        for (int i = 0; i < params.size(); i++) {
            final NameValuePair param = params.get(i);
            if (!param.getName().equals(keys.get(i))) {
                throw new IllegalArgumentException("globalId is invalid, expected format is: " + getExpectedFormat(keys) + ", found: " + globalId);
            }

            result.put(param.getName(), param.getValue());
        }

        return result;
    }

    private static String getExpectedFormat(final List<String> keys) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (final String key : keys) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }

            sb.append(key).append("=<").append(key).append(">");
        }

        return sb.toString();
    }


    private Project getProjectFromIssueBean(final CopyIssueBean copyIssueBean) throws ProjectNotFoundException {
        ProjectService.GetProjectResult result = projectService.getProjectByKey(callingUser(), copyIssueBean.getTargetProjectKey());
        if (!result.isValid()) {
            throw new ProjectNotFoundException(result.getErrorCollection());
        }
        return result.getProject();
    }


    private IssueType findIssueType(final String issueType, final Project project) {
        Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
        try {
            return Iterables.find(issueTypesForProject, new Predicate<IssueType>() {
                public boolean apply(final IssueType input) {
                    return input.getName().equals(issueType);
                }
            });
        } catch (NoSuchElementException ex) {
            throw new RESTException(Response.Status.NOT_FOUND, "No issue type with name '" + issueType + "' found!");
        }
    }


    private User callingUser() {
        return authenticationContext.getLoggedInUser();
    }


}
