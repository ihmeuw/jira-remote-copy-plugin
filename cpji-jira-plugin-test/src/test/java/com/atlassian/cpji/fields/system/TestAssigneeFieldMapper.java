package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class TestAssigneeFieldMapper
{
    AssigneeFieldMapper asigneeFieldMapper;
    //components
    @Mock
    UserMappingManager userMappingManager;
    @Mock
    CachingUserMapper userMapper;
    @Mock
    PermissionManager permissionManager;
    @Mock
    ApplicationProperties applicationProperties;
    @Mock
    DefaultFieldValuesManager defaultValuesManager;

    //data objects
    @Mock
    Project project;
    @Mock
    UserBean userBean;
    @Mock
    ApplicationUser projectLead;
    @Mock
    ApplicationUser mappedUser;
    @Mock
    IssueInputParameters inputParams;
    @Mock
    OrderableField asigneeField;

    boolean plugedMapMethod = false;

    AssigneeFieldMapper.InternalMappingResult pluggedMappingResult;

    @Before
    public void setUp()
    {
        plugedMapMethod = false;

        FieldManager fieldManager = mock(FieldManager.class);
        when(asigneeField.getId()).thenReturn(IssueFieldConstants.ASSIGNEE);
        when(fieldManager.getField(IssueFieldConstants.ASSIGNEE)).thenReturn(asigneeField);

        asigneeFieldMapper = new AssigneeFieldMapper(permissionManager, applicationProperties, fieldManager, defaultValuesManager)
        {
            @Override
            public InternalMappingResult mapUser(CachingUserMapper userMapper, UserBean user, Project project)
            {
                if (plugedMapMethod)
                {
                    return pluggedMappingResult;
                }
                else
                { return super.mapUser(userMapper, user, project); }
            }
        };
    }

    //mappings

    @Test
    public void mappingShouldReturnNotFoundWhenNoUserBeanGiven()
    {
        mapAndTest(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND, null);
    }

    @Test
    public void mappingShouldReturnNotFoundWhenUserCannotBeMappedAndDefaulAssigneeIsNotSet()
    {
        userIsNotMapped();

        mapAndTest(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND);
    }

    @Test
    public void mappingShouldReturnMappedAssigneeWhenIsMappableAndHasPermission()
    {
        userIsWellMapped();
        userCanBeAssigned();

        mapAndTest(mappedUser, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.FOUND);
    }

    @Test
    public void mappingShouldReturnNotFoundWhenUserIsMappedAndHasNoPermission()
    {
        userIsWellMapped();
        userCannotBeAssigned();
        mapAndTest(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND);
    }

    void mapAndTest(ApplicationUser desiredUser, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision desiredDecision)
    {
        mapAndTest(desiredUser, desiredDecision, userBean);
    }

    void mapAndTest(ApplicationUser desiredUser, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision desiredDecision, UserBean userBean)
    {
        final AssigneeFieldMapper.InternalMappingResult result = asigneeFieldMapper.mapUser(userMapper, userBean, project);
        assertEquals(desiredUser, result.mappedUser);
        assertEquals(desiredDecision, result.decision);
    }

    void userIsNotMapped() { when(userMapper.mapUser(userBean)).thenReturn(null); }

    void userIsWellMapped() { when(userMapper.mapUser(userBean)).thenReturn(mappedUser); }

    void userCanBeAssigned() { when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(true); }

    void userCannotBeAssigned() { when(permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, mappedUser)).thenReturn(false); }

    //population

    @Test
    public void shouldPopulateParametersWhenUserIsFound()
    {

        CopyIssueBean bean = new CopyIssueBean();
        bean.setAssignee(userBean);

        when(mappedUser.getName()).thenReturn("fred");
        plugMapMethod(new AssigneeFieldMapper.InternalMappingResult(mappedUser, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.FOUND));
        asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, null, null, null);
        verify(inputParams).setAssigneeId("fred");
    }

    @Test
    public void shouldDoNothingWhenMappingIsNotFoundAndUnassignedAreAllowed()
    {
        CopyIssueBean bean = new CopyIssueBean();
        bean.setAssignee(userBean);

        plugMapMethod(new AssigneeFieldMapper.InternalMappingResult(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND));
        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(true);

        asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, null, project, null);
        verify(inputParams, never()).setAssigneeId(any());
    }

    @Test
    public void shouldPopulateWithDefaultValueWhenItIsSet()
    {
        CopyIssueBean bean = new CopyIssueBean();
        bean.setAssignee(userBean);

        plugMapMethod(new AssigneeFieldMapper.InternalMappingResult(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND));
        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(false);

        final FieldLayoutItem fieldLayoutItem = mock(FieldLayoutItem.class);
        when(fieldLayoutItem.getOrderableField()).thenReturn(asigneeField);
        when(defaultValuesManager.hasDefaultValue(any(), any(), any())).thenReturn(true);
        when(defaultValuesManager.getDefaultFieldValue(any(), any(), any())).thenReturn(new String[] { "defaultAsgn" });

        asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, fieldLayoutItem, project, null);
        verify(inputParams).setAssigneeId("defaultAsgn");
    }

    @Test
    public void shouldPopulateWithAutomaticAssigneeWhenNoDefaultValueIsDefined()
    {
        CopyIssueBean bean = new CopyIssueBean();
        bean.setAssignee(userBean);

        plugMapMethod(new AssigneeFieldMapper.InternalMappingResult(null, AssigneeFieldMapper.InternalMappingResult.MappingResultDecision.NOT_FOUND));
        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED)).thenReturn(false);

        FieldLayoutItem fieldLayoutItem = mock(FieldLayoutItem.class);
        when(defaultValuesManager.hasDefaultValue(any(), any(), any())).thenReturn(false);

        asigneeFieldMapper.populateInputParams(userMapper, inputParams, bean, fieldLayoutItem, project, null);
        verify(inputParams).setAssigneeId("-1"); //jira should choose assignee automatically
    }

    public void plugMapMethod(AssigneeFieldMapper.InternalMappingResult result)
    {
        pluggedMappingResult = result;
        plugedMapMethod = true;
    }

}
