package com.atlassian.cpji;

import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.exceptions.IssueCreatedWithErrorsException;
import com.atlassian.cpji.components.exceptions.ValidationException;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapper;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.system.FieldCreationException;
import com.atlassian.cpji.fields.system.SystemFieldPostIssueCreationFieldMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import com.atlassian.cpji.components.InputParametersService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.fugue.Pair;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestCopyIssueService {

    public static final String ISSUE_TYPE = "Issue type";
    public static final String PROJECT_KEY = "PKEY";
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
    private DefaultFieldValuesManager defaultFieldValuesManager;
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

    @Before
    public void setUp() throws Exception {
        JiraAuthenticationContext authenticationContext = mock(JiraAuthenticationContext.class);
        when(authenticationContext.getLoggedInUser()).thenReturn(currentUser);

        when(issueType.getName()).thenReturn(ISSUE_TYPE);
        when(issueTypeSchemeManager.getIssueTypesForProject(project)).thenReturn(ImmutableList.of(issueType));
        when(project.getKey()).thenReturn(PROJECT_KEY);
        when(projectService.getProjectByKey(currentUser, PROJECT_KEY)).thenReturn( new ProjectService.GetProjectResult(project));

        when(createdIssue.getKey()).thenReturn("PKEY-4");
        when(createdIssue.getId()).thenReturn(1L);

        copyIssueService = new CopyIssueService(issueService, authenticationContext, projectService, issueTypeSchemeManager, fieldLayoutManager, fieldMapperFactory, fieldManager, defaultFieldValuesManager, fieldLayoutItemsRetriever, internalHostApplication, issueLinkService, remoteIssueLinkService, inputParametersService);
    }


    @Test()
    public void copyIssuePopulatesInputParamsAndCreatesIssue() throws Exception {
        final InputParametersService.Populator populator = mock(InputParametersService.Populator.class);
        final FieldLayoutItem item = mock(FieldLayoutItem.class);
        CopyIssueBean bean = prepareCopyRequirements(populator, item);
        prepareIssueService();

        IssueCreationResultBean result = copyIssueService.copyIssue(bean);

        verify(populator).populateProjectSystemField();
        verify(populator, times(3)).injectInputParam(item);
        verify(issueService).validateCreate(currentUser, null);
        verify(issueService).create(eq(currentUser), (IssueService.CreateValidationResult) any());
        assertEquals(project.getKey(), result.getProject());
        assertEquals(createdIssue.getKey(), result.getIssueKey());
        assertEquals(createdIssue.getId(), result.getIssueId());
    }

    @Test(expected = IssueCreatedWithErrorsException.class)
    public void copyIssueWithValidationErrors() throws Exception{
        final InputParametersService.Populator populator = mock(InputParametersService.Populator.class);
        final FieldLayoutItem item = mock(FieldLayoutItem.class);
        CopyIssueBean bean = prepareCopyRequirements(populator, item);
        Pair<IssueService.CreateValidationResult,IssueService.IssueResult> validations = prepareIssueService();

        SystemFieldPostIssueCreationFieldMapper mapperWithException = mock(SystemFieldPostIssueCreationFieldMapper.class);
        SystemFieldPostIssueCreationFieldMapper mapper = mock(SystemFieldPostIssueCreationFieldMapper.class);
        doThrow(new FieldCreationException("msg", "fieldId")).when(mapperWithException).process(createdIssue, bean);

        when(fieldMapperFactory.getPostIssueCreationFieldMapper()).thenReturn(ImmutableList.of(
                mapper,mapperWithException
        ));

        copyIssueService.copyIssue(bean);

        verify(mapper).process(createdIssue, bean);
        verify(mapperWithException).process(createdIssue, bean);
    }

    @Test(expected = ValidationException.class)
    public void copyIssueThrowsExceptionOnUnsuccessfulValidation() throws Exception{
        final InputParametersService.Populator populator = mock(InputParametersService.Populator.class);
        final FieldLayoutItem item = mock(FieldLayoutItem.class);
        CopyIssueBean bean = prepareCopyRequirements(populator, item);
        Pair<IssueService.CreateValidationResult,IssueService.IssueResult> validations = prepareIssueService();

        when(validations.left().isValid()).thenReturn(false);
        copyIssueService.copyIssue(bean);
    }


    private CopyIssueBean prepareCopyRequirements(InputParametersService.Populator populator, FieldLayoutItem item){
        CopyIssueBean bean = new CopyIssueBean();
        bean.setTargetProjectKey(PROJECT_KEY);
        bean.setTargetIssueType(ISSUE_TYPE);

        //returning field populator
        ImmutableMap<String, FieldMapper> fields = ImmutableMap.of();
        when(fieldMapperFactory.getSystemFieldMappers()).thenReturn(fields);
        when(inputParametersService.getFieldsPopulator(project, issueType, bean, fields)).thenReturn(populator);

        //returning all fields
        when(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(project, issueType)).thenReturn(ImmutableList.of(item, item, item));

        return bean;
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
