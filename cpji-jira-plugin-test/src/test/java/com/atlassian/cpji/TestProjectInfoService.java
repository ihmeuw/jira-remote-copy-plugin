package com.atlassian.cpji;

import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.IssueTypeBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.webtests.Permissions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestProjectInfoService {

    
    private ProjectInfoService projectInfoService;

    @Mock
    private ProjectService projectService;
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
        projectInfoService = new ProjectInfoService(projectService, issueTypeSchemeManager, applicationProperties, jiraAuthenticationContext, permissionManager, buildUtilsInfo,
				fieldLayoutItemsRetriever);
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(mockedUser);
    }

    @Test
    public void getIssueTypeInformationShouldCheckPermissions() throws Exception {
        Project project = mock(Project.class);
        when(projectService.getProjectByKey(mockedUser, "KEY")).thenReturn(new ProjectService.GetProjectResult(project));

        when(permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, mockedUser)).thenReturn(true);

        ImmutableList<String> issueTypeNames = ImmutableList.of("ISSUETYPE1", "Another type");
        List<IssueType> issueTypes = ImmutableList.copyOf(Iterables.transform(issueTypeNames, new Function<String, IssueType>() {
            @Override
            public IssueType apply(@Nullable String input) {
                return mockIssueType(input);
            }
        }));

        when(issueTypeSchemeManager.getIssueTypesForProject(project)).thenReturn(issueTypes);

        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS)).thenReturn(true);
        when(applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING)).thenReturn(true);

        CopyInformationBean result = projectInfoService.getIssueTypeInformation("KEY");

        assertEquals(true, result.getHasCreateIssuePermission());
        assertEquals(false, result.getHasCreateAttachmentPermission());
        assertThat(Iterables.transform(result.getIssueTypes(), new Function<IssueTypeBean, String>() {
			@Override
			public String apply(IssueTypeBean input) {
				return input.getName();
			}
		}), IsIterableContainingInAnyOrder.<String> containsInAnyOrder(issueTypeNames.toArray(new String[] {})));
        assertEquals(true, result.getAttachmentsEnabled());
        assertEquals(true, result.getIssueLinkingEnabled());
        verify(permissionManager).hasPermission(Permissions.CREATE_ISSUE, project, mockedUser);
        verify(permissionManager).hasPermission(Permissions.CREATE_ATTACHMENT, project, mockedUser);
    }

    private IssueType mockIssueType(String name){
        IssueType type = mock(IssueType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }


    
    
}
