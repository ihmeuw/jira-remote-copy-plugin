package com.atlassian.cpji;

import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.webtests.Permissions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.*;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestProjectInfoService {


    private ProjectInfoService projectInfoService;

    @Mock
    private ProjectManager projectManager;
    @Mock
    private IssueTypeSchemeManager issueTypeSchemeManager;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private BuildUtilsInfo buildUtilsInfo;
    @Mock
    private User mockedUser;
    @Mock
    private FieldLayoutItemsRetriever fieldLayoutItemsRetriever;

    @Before
    public void setUp() throws Exception {
        projectInfoService = new ProjectInfoService(projectManager, issueTypeSchemeManager, applicationProperties, jiraAuthenticationContext, permissionManager, buildUtilsInfo,
                fieldLayoutItemsRetriever);
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(mockedUser);
        when(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(any(Project.class), any(IssueType.class))).thenReturn(Collections.<FieldLayoutItem>emptyList());
    }

    @Test
    public void getIssueTypeInformationShouldCheckPermissions() throws Exception {
        Project project = new MockProject(3L, "KEY");
        when(projectManager.getProjectObjByKey("KEY")).thenReturn(project);
        when(permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, mockedUser, true)).thenReturn(true);

        ImmutableList<IssueType> issueTypes = ImmutableList.of(mockIssueType("ISSUETYPE1"), mockIssueType("Another type"));
        ImmutableList<IssueType> subTaskIssueTypes = ImmutableList.of(mockIssueType("sub1"), mockIssueType("subtask2"));

        when(issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(project)).thenReturn(issueTypes);
        when(issueTypeSchemeManager.getSubTaskIssueTypesForProject(project)).thenReturn(subTaskIssueTypes);

        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS)).thenReturn(true);
        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING)).thenReturn(true);
        when(applicationProperties.getDefaultBackedString(APKeys.JIRA_ATTACHMENT_SIZE)).thenReturn("1000");

        CopyInformationBean result = projectInfoService.getIssueTypeInformation("KEY");

        assertEquals(false, result.getHasCreateAttachmentPermission());
        assertThat(
                Iterables.transform(result.getIssueTypes(), issueTypeBeanToName()),
                hasItems(Iterables.toArray(Iterables.transform(issueTypes, issueTypeToName()), String.class))
        );
        assertThat(
                Iterables.transform(result.getSubtaskIssueTypes(), issueTypeBeanToName()),
                hasItems(Iterables.toArray(Iterables.transform(subTaskIssueTypes, issueTypeToName()), String.class))
        );
        assertEquals(true, result.getAttachmentsEnabled());
        assertEquals(true, result.getIssueLinkingEnabled());
        assertEquals(1000L, result.getMaxAttachmentSize().longValue());
        verify(permissionManager).hasPermission(Permissions.CREATE_ATTACHMENT, project, mockedUser);
    }

    @Test
    public void getProjectForCreateIssueShouldReturnProjectWhenItHasCreateIssuePermission() throws Exception {
        Project project = new MockProject(2L, "MyKey");
        when(projectManager.getProjectObjByKey("MyKey")).thenReturn(project);
        when(permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, mockedUser, true)).thenReturn(true);

        final Project found = projectInfoService.getProjectForCreateIssue("MyKey");
        assertEquals(2L, found.getId().longValue());
    }

    @Test(expected = ProjectNotFoundException.class)
    public void getProjectForCreateIssueShouldThrowProjectNotFoundExceptionWhenProjectIsNotFound() throws Exception {
        when(projectManager.getProjectObjByKey("MyKey")).thenReturn(null);
        projectInfoService.getProjectForCreateIssue("MyKey");
    }

    @Test(expected = ProjectNotFoundException.class)
    public void getProjectForCreateIssueShouldThrowProjectNotFoundExceptionWhenUserDoesntHaveCreateIssuePermission() throws Exception {
        Project project = new MockProject(2L, "MyKey");
        when(projectManager.getProjectObjByKey("MyKey")).thenReturn(project);
        when(permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, mockedUser, true)).thenReturn(false);
        projectInfoService.getProjectForCreateIssue("MyKey");
    }


    private IssueType mockIssueType(String name) {
        IssueType type = mock(IssueType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }

    public static Function<IssueTypeBean, String> issueTypeBeanToName() {
        return new Function<IssueTypeBean, String>() {
            @Override
            public String apply(@Nullable IssueTypeBean input) {
                return input.getName();
            }
        };
    }

    public static Function<IssueType, String> issueTypeToName() {
        return new Function<IssueType, String>() {
            @Override
            public String apply(@Nullable IssueType input) {
                return input.getName();
            }
        };
    }


}
