package com.atlassian.cpji;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.InputParametersService;
import com.atlassian.cpji.components.exceptions.IssueCreatedWithErrorsException;
import com.atlassian.cpji.components.exceptions.ValidationException;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.permission.CustomFieldMappingChecker;
import com.atlassian.cpji.fields.permission.SystemFieldMappingChecker;
import com.atlassian.cpji.fields.system.FieldCreationException;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.rest.model.*;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.fugue.Pair;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestCopyIssueService {

    public static final String ISSUE_TYPE = "Issue type";
    public static final String PROJECT_KEY = "PKEY";
    public static final String ISSUE_KEY = "PKEY-4";
    public static final Long ISSUE_ID = 1L;

    private CopyIssueService copyIssueService;

    @Mock
    private IssueService issueService;
    @Mock
    private ProjectService projectService;
    @Mock
    private IssueTypeSchemeManager issueTypeSchemeManager;
    @Mock
    private FieldLayoutManager fieldLayoutManager;
    @Mock
    private FieldMapperFactory fieldMapperFactory;
    @Mock
    private FieldManager fieldManager;
    @Mock
    private FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    @Mock
    private InternalHostApplication internalHostApplication;
    @Mock
    private IssueLinkService issueLinkService;
    @Mock
    private RemoteIssueLinkService remoteIssueLinkService;
    @Mock
    private InputParametersService inputParametersService;
    @Mock
    private User currentUser;
    @Mock
    private IssueType issueType;
    @Mock
    private Project project;
    @Mock
    private MutableIssue createdIssue;
    @Mock
    private I18nHelper i18nHelper;

    @Before
    public void setUp() throws Exception {
        JiraAuthenticationContext authenticationContext = mock(JiraAuthenticationContext.class);
        when(authenticationContext.getLoggedInUser()).thenReturn(currentUser);

        when(issueType.getName()).thenReturn(ISSUE_TYPE);
        when(issueTypeSchemeManager.getIssueTypesForProject(project)).thenReturn(ImmutableList.of(issueType));
        when(project.getKey()).thenReturn(PROJECT_KEY);
        when(projectService.getProjectByKey(currentUser, PROJECT_KEY)).thenReturn( new ProjectService.GetProjectResult(project));

        when(createdIssue.getKey()).thenReturn(ISSUE_KEY);
        when(createdIssue.getId()).thenReturn(ISSUE_ID);

        copyIssueService = new CopyIssueService(issueService, authenticationContext, projectService, issueTypeSchemeManager, fieldLayoutManager, fieldMapperFactory, fieldManager, fieldLayoutItemsRetriever, internalHostApplication, issueLinkService, remoteIssueLinkService, inputParametersService, i18nHelper);
    }


    @Test()
    public void copyIssuePopulatesInputParamsAndCreatesIssue() throws Exception {
        CopyRequirements reqs = new CopyRequirements();
        prepareIssueService();

        IssueCreationResultBean result = copyIssueService.copyIssue(reqs.copyBean);

        verify(reqs.fieldPopulator).populateProjectSystemField();
        verify(reqs.fieldPopulator, times(3)).injectInputParam(reqs.genericFieldLayoutItem);
        verify(issueService).validateCreate(currentUser, null);
        verify(issueService).create(eq(currentUser), (IssueService.CreateValidationResult) any());
        assertEquals(project.getKey(), result.getProject());
        assertEquals(ISSUE_KEY, result.getIssueKey());
        assertEquals(ISSUE_ID, result.getIssueId());
    }

    @Test(expected = IssueCreatedWithErrorsException.class)
    public void copyIssueWithPostCreationErrors() throws Exception{
        CopyRequirements reqs = new CopyRequirements();
        prepareIssueService();

        SystemFieldPostIssueCreationFieldMapper mapperWithException = mock(SystemFieldPostIssueCreationFieldMapper.class);
        SystemFieldPostIssueCreationFieldMapper mapper = mock(SystemFieldPostIssueCreationFieldMapper.class);
        when(mapperWithException.getFieldId()).thenReturn("fieldWithExceptionId");
        doThrow(new FieldCreationException("field creation message", "fieldId")).when(mapperWithException).process(createdIssue, reqs.copyBean);

        when(fieldMapperFactory.getPostIssueCreationFieldMapper()).thenReturn(ImmutableList.of(
                mapper,mapperWithException
        ));

        copyIssueService.copyIssue(reqs.copyBean);

        verify(mapper).process(createdIssue, reqs.copyBean);
        verify(mapperWithException).process(createdIssue, reqs.copyBean);
    }

    @Test(expected = ValidationException.class)
    public void copyIssueThrowsExceptionOnUnsuccessfulValidation() throws Exception{
        CopyRequirements reqs = new CopyRequirements();
        Pair<IssueService.CreateValidationResult,IssueService.IssueResult> validations = prepareIssueService();

        when(validations.left().isValid()).thenReturn(false);
        copyIssueService.copyIssue(reqs.copyBean);
    }


    @Test
    public void checkFilePermissionsShouldFindAllUnmappedFieldsAndAddThemToResult() throws Exception {
        CopyRequirements reqs = new CopyRequirements();

        FieldCheckers checkers = new FieldCheckers(reqs.copyBean).withBeansAsUnmapped();

        FieldPermissionsBean result = copyIssueService.checkFieldPermissions(reqs.copyBean);

        verify(checkers.system).findUnmappedRemoteFields(eq(reqs.copyBean), Matchers.<Iterable<FieldLayoutItem>>any());
        verify(checkers.custom).findUnmappedRemoteFields(eq(reqs.copyBean), Matchers.<Iterable<FieldLayoutItem>>any());

        assertEquals(result.getSystemFieldPermissionBeans(), ImmutableList.of(checkers.systemFieldPermissionBean));
        assertEquals(result.getCustomFieldPermissionBeans(), ImmutableList.of(checkers.customFieldPermissionBean));
    }

    @Test
    public void checkFilePermissionsShouldCheckPermissionsForMappedFields() throws Exception{
        CopyRequirements reqs = new CopyRequirements();

        when(fieldManager.isCustomField("1")).thenReturn(false);
        when(fieldManager.isCustomField("2")).thenReturn(true);
        when(fieldManager.isCustomField("3")).thenReturn(true);

        FieldCheckers checkers = new FieldCheckers(reqs.copyBean).withBeansForCustomFields("2").withBeansForSystemFields("1");

        FieldPermissionsBean result = copyIssueService.checkFieldPermissions(reqs.copyBean);

        assertEquals(result.getSystemFieldPermissionBeans(), ImmutableList.of(checkers.systemFieldPermissionBean));
        assertEquals(result.getCustomFieldPermissionBeans(), ImmutableList.of(checkers.customFieldPermissionBean));

    }

    @Test
    public void testConvertRemoteLinks() throws Exception{
        final String applicationId = "db60eb28-51aa-3f22-b3cc-b8967fa6281b";

        //our issue
        when(issueService.getIssue(currentUser, ISSUE_KEY)).thenReturn(new IssueService.IssueResult(createdIssue));

        //connected issues we want to connect with local links
        MutableIssue issue123 = registerIssue(123L);
        MutableIssue issue4567 = registerIssue(4567L);

        //preparing links list
        RemoteIssueLinkService.RemoteIssueLinkListResult linksResult = mock(RemoteIssueLinkService.RemoteIssueLinkListResult.class);
        when(remoteIssueLinkService.getRemoteIssueLinksForIssue(currentUser, createdIssue)).thenReturn(linksResult);
        when(linksResult.isValid()).thenReturn(true);
        RemoteIssueLink firstLink = buildRemoteIssueLink(applicationId, "123", 555L);
        RemoteIssueLink secondLink = buildRemoteIssueLink(applicationId, "4567", 444L);
        when(linksResult.getRemoteIssueLinks()).thenReturn(ImmutableList.of(firstLink, secondLink));

        when(internalHostApplication.getId()).thenReturn(new ApplicationId(applicationId));

        //preparing issue link adding validation results
        IssueLinkService.AddIssueLinkValidationResult add123Result = mock(IssueLinkService.AddIssueLinkValidationResult.class);
        when(add123Result.isValid()).thenReturn(true);
        IssueLinkService.AddIssueLinkValidationResult add4567Result = mock(IssueLinkService.AddIssueLinkValidationResult.class);
        when(add4567Result.isValid()).thenReturn(true);
        when(issueLinkService.validateAddIssueLinks(currentUser, createdIssue, "strangeRelationship", ImmutableList.of("KEY-123"))).thenReturn(add123Result);
        when(issueLinkService.validateAddIssueLinks(currentUser, createdIssue, "strangeRelationship", ImmutableList.of("KEY-4567"))).thenReturn(add4567Result);

        //preparing delation validation results
        RemoteIssueLinkService.DeleteValidationResult deleteValidationResult = mock(RemoteIssueLinkService.DeleteValidationResult.class);
        when(remoteIssueLinkService.validateDelete(currentUser, 555L)).thenReturn(deleteValidationResult);
        when(remoteIssueLinkService.validateDelete(currentUser, 444L)).thenReturn(deleteValidationResult);
        when(deleteValidationResult.isValid()).thenReturn(true).thenReturn(false);

        copyIssueService.convertRemoteLinksToLocal(ISSUE_KEY);

        verify(issueLinkService).addIssueLinks(currentUser, add123Result);
        verify(issueLinkService).addIssueLinks(currentUser, add4567Result);

        verify(remoteIssueLinkService).delete(currentUser, deleteValidationResult);




    }

    private MutableIssue registerIssue(Long id){
        MutableIssue issue = mock(MutableIssue.class);
        final String key = "KEY-" + id.toString();
        when(issue.getKey()).thenReturn(key);
        when(issueService.getIssue(currentUser, id)).thenReturn(new IssueService.IssueResult(issue));
        when(issueService.getIssue(currentUser, key)).thenReturn(new IssueService.IssueResult(issue));
        return issue;
    }

    private RemoteIssueLink buildRemoteIssueLink(String appId, String issueId, Long linkId){
        return new RemoteIssueLinkBuilder()
                .applicationType(RemoteIssueLink.APPLICATION_TYPE_JIRA)
                .relationship("strangeRelationship")
                .id(linkId)
                .globalId(String.format("appId=%s&issueId=%s", appId, issueId)).build();
    }




    private class FieldCheckers {
        private final SystemFieldMappingChecker system;
        private final CustomFieldMappingChecker custom;

        private final SystemFieldPermissionBean systemFieldPermissionBean;
        private final CustomFieldPermissionBean customFieldPermissionBean;
        private final CopyIssueBean bean;

        public FieldCheckers(CopyIssueBean bean){
            this.bean = bean;
            system = mock(SystemFieldMappingChecker.class);
            custom = mock(CustomFieldMappingChecker.class);
            systemFieldPermissionBean = mock(SystemFieldPermissionBean.class);
            customFieldPermissionBean = mock(CustomFieldPermissionBean.class);

            when(inputParametersService.getSystemFieldMappingChecker(eq(project), eq(bean), Matchers.<FieldLayout>any())).thenReturn(system);
            when(inputParametersService.getCustomFieldMappingChecker(eq(project), eq(bean), Matchers.<FieldLayout>any())).thenReturn(custom);
        }

        public FieldCheckers withBeansAsUnmapped(){
            when(system.findUnmappedRemoteFields(eq(bean), Matchers.<Iterable<FieldLayoutItem>>any())).thenReturn(ImmutableList.of(systemFieldPermissionBean));
            when(custom.findUnmappedRemoteFields(eq(bean), Matchers.<Iterable<FieldLayoutItem>>any())).thenReturn(ImmutableList.of(customFieldPermissionBean));
            return this;
        }

        public FieldCheckers withBeansForCustomFields(String ... ids){
            for (String id : ids){
                when(custom.getFieldPermission(id)).thenReturn(customFieldPermissionBean);
            }
            return this;
        }

        public FieldCheckers withBeansForSystemFields(String ... ids){
            for (String id : ids){
                when(system.getFieldPermission(id)).thenReturn(systemFieldPermissionBean);
            }
            return this;
        }


    }

    private class CopyRequirements{
        private final CopyIssueBean copyBean;
        private final InputParametersService.Populator fieldPopulator;
        private final FieldLayoutItem genericFieldLayoutItem;

        private CopyRequirements(){
            fieldPopulator = mock(InputParametersService.Populator.class);
            genericFieldLayoutItem = mock(FieldLayoutItem.class);
            OrderableField orderableField = mock(OrderableField.class);
            when(orderableField.getId()).thenReturn("1").thenReturn("2").thenReturn("3");
            when(genericFieldLayoutItem.getOrderableField()).thenReturn(orderableField);


            copyBean = new CopyIssueBean();
            copyBean.setTargetProjectKey(PROJECT_KEY);
            copyBean.setTargetIssueType(ISSUE_TYPE);

            //returning field populator
            ImmutableMap<String, FieldMapper> fields = ImmutableMap.of();
            when(fieldMapperFactory.getSystemFieldMappers()).thenReturn(fields);
            when(inputParametersService.getFieldsPopulator(project, issueType, copyBean, fields)).thenReturn(fieldPopulator);

            //returning all field items
            when(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType)).thenReturn(ImmutableList.of(genericFieldLayoutItem, genericFieldLayoutItem, genericFieldLayoutItem));
        }


    }



    private Pair<IssueService.CreateValidationResult, IssueService.IssueResult> prepareIssueService(){
        //validation result
        IssueService.CreateValidationResult validationResult = mock(IssueService.CreateValidationResult.class);
        when(validationResult.isValid()).thenReturn(true);
        when(issueService.validateCreate(currentUser, null)).thenReturn(validationResult);

        //creation result
        IssueService.IssueResult issueResult = mock(IssueService.IssueResult.class);
        when(issueResult.isValid()).thenReturn(true);
        when(issueResult.getIssue()).thenReturn(createdIssue);
        when(createdIssue.getProjectObject()).thenReturn(project);
        when(issueService.create(currentUser, validationResult)).thenReturn(issueResult);


        return Pair.pair(validationResult, issueResult);
    }

}
