package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.ProjectInfoService;
import com.atlassian.cpji.components.exceptions.ProjectNotFoundException;
import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.PluginVersion;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.model.ResultWithJiraLocation;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.rest.PluginInfoResource;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.FieldPermissionsBean;
import com.atlassian.cpji.rest.model.IssueCreationResultBean;
import io.atlassian.fugue.Either;
import io.atlassian.fugue.Pair;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.AttachmentError;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Objects;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestLocalJiraProxy {

    public static final String LOCAL_JIRA_NAME = "localJiraName";
    public static final String PROJECT_KEY = "KEY";
    @Mock private PermissionManager permissionManager;
    @Mock private JiraAuthenticationContext jiraAuthenticationContext;
    @Mock private CopyIssueService copyIssueService;
    @Mock private AttachmentManager attachmentManager;
    @Mock private IssueManager issueManager;
    @Mock private IssueLinkManager issueLinkManager;
    @Mock private RemoteIssueLinkManager remoteIssueLinkManager;
    @Mock private ProjectInfoService projectInfoService;
    @Mock private JiraBaseUrls jiraBaseUrls;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private ApplicationUser currentUser;
    @Mock private ApplicationUser currentApplicationUser;

    private final SimpleErrorCollection errors = new SimpleErrorCollection();

    private LocalJiraProxy localJiraProxy;

    @Before
    public void setUp() throws Exception {
        when(applicationProperties.getString(anyString())).thenReturn(LOCAL_JIRA_NAME);
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(currentUser);
        when(jiraAuthenticationContext.getUser()).thenReturn(currentApplicationUser);

        localJiraProxy = new LocalJiraProxy( permissionManager, jiraAuthenticationContext, copyIssueService,
                attachmentManager, issueManager, issueLinkManager, remoteIssueLinkManager,
                projectInfoService, jiraBaseUrls,applicationProperties );
    }

    @Test
    public void jiraLocationContainsKeyAndAppTitle(){
        JiraLocation location = localJiraProxy.getJiraLocation();
        assertEquals(JiraLocation.LOCAL.getId(), location.getId());
        assertEquals(LOCAL_JIRA_NAME, location.getName());
    }

    @Test
    public void getProjectsReturnsProjectsList(){

        when(permissionManager.getProjects(ProjectPermissions.CREATE_ISSUES, currentApplicationUser)).thenReturn(ImmutableList.<Project>of(
                new MockProject(0, "KEY0", "Name 0"),
                new MockProject(1, "KEY1", "Name 1")
        ));

        Projects projects = extractResult(localJiraProxy.getProjects());

        assertEquals(2, Iterables.size(projects.getResult()));

        Iterable<Pair<String, String>> keysAndNames = Iterables.transform(projects.getResult(), input -> Pair.pair(input.getKey(), input.getName()));
        assertThat(keysAndNames, containsInAnyOrder(Pair.pair("KEY0", "Name 0"), Pair.pair("KEY1", "Name 1")));
    }

    @Test
    public void testPluginInstalled(){
        final Either<NegativeResponseStatus, PluginVersion> pluginInstalled = localJiraProxy.isPluginInstalled();
        assertEquals(PluginInfoResource.PLUGIN_VERSION, pluginInstalled.right().get().getResult());
    }

    @Test
    public void urlGenerationReturnsNull() {
        assertNull(localJiraProxy.generateAuthenticationUrl(null));
    }

    @Test
    public void getCopyInformationShouldReturnResponseStatusOnException() throws Exception {
        when(projectInfoService.getIssueTypeInformation(PROJECT_KEY)).thenThrow(new ProjectNotFoundException(errors));

        verifyFailure(localJiraProxy.getCopyInformation(PROJECT_KEY), errors);
    }

    @Test
    public void getCopyInformationShouldGetDataFromService() throws Exception {
        CopyInformationBean bean = new CopyInformationBean();
        when(projectInfoService.getIssueTypeInformation(PROJECT_KEY)).thenReturn(bean);

        CopyInformationBean copyResult = extractRight(localJiraProxy.getCopyInformation(PROJECT_KEY));

        assertSame(bean, copyResult);
    }

    @Test
    public void copyIssueShouldReturnResponseStatusOnException() throws Exception {
        CopyIssueBean bean = new CopyIssueBean();

        when(copyIssueService.copyIssue(bean)).thenThrow(new ProjectNotFoundException(errors));

        verifyFailure(localJiraProxy.copyIssue(bean), errors);

    }

    @Test
    public void copyIssueShouldSetParentIdOnCloningSubtaskToTheSameProject() throws Exception {
        CopyIssueBean bean = new CopyIssueBean();
        bean.setOriginalKey("KEY-ORIGINAL");
        bean.setTargetProjectKey("TARGET");

        MutableIssue clonedIssue = new MockIssue();
        final MockProject project = new MockProject();
        project.setKey("TARGET");
        clonedIssue.setProjectObject(project);
        clonedIssue.setParentId(123L);

        when(projectInfoService.isIssueTypeASubtask(null, "TARGET")).thenReturn(true);
        when(issueManager.getIssueObject("KEY-ORIGINAL")).thenReturn(clonedIssue);
        when(copyIssueService.copyIssue(Matchers.<CopyIssueBean>any())).thenReturn(new IssueCreationResultBean());

        localJiraProxy.copyIssue(bean);

        ArgumentMatcher<CopyIssueBean> containsParentIssueId = argument -> argument.getTargetParentId() == 123L;

        verify(copyIssueService).copyIssue(argThat(containsParentIssueId));
    }


    @Test
    public void copyIssueShouldUseService() throws Exception {
        IssueCreationResultBean bean = new IssueCreationResultBean();
        CopyIssueBean copyIssueBean = new CopyIssueBean();
        when(copyIssueService.copyIssue(copyIssueBean)).thenReturn(bean);

        IssueCreationResultBean result = extractRight(localJiraProxy.copyIssue(copyIssueBean));

        assertSame(bean, result);
        verify(copyIssueService).copyIssue(copyIssueBean);
    }


    @Test
    public void testAddAttachment() throws Exception {
        Attachment attachment = mock(Attachment.class);
        when(attachmentManager.copyAttachment(eq(attachment), eq(currentApplicationUser), eq("key"))).thenReturn(Either.<AttachmentError, Attachment>right(attachment));
        verifySuccess(localJiraProxy.addAttachment("key", attachment));
        verify(attachmentManager).copyAttachment(eq(attachment), eq(currentApplicationUser), eq("key"));
    }

    @Test
    public void addAttachmentShouldReturnResponseStatusOnException() throws Exception {
        when(attachmentManager.copyAttachment(Matchers.<Attachment>any(), Matchers.<ApplicationUser>any(), Matchers.<String>any()))
                .thenReturn(Either.<AttachmentError, Attachment>left(new AttachmentError("message", null, null, ErrorCollection.Reason.SERVER_ERROR)));

        verifyFailure(localJiraProxy.addAttachment(null, null));
    }

    @Test
    public void testCheckPermissions() throws Exception{
        FieldPermissionsBean bean = new FieldPermissionsBean();
        when(copyIssueService.checkFieldPermissions(null)).thenReturn(bean);

        FieldPermissionsBean result = extractRight(localJiraProxy.checkPermissions(null));
        assertSame(bean, result);
    }

    @Test
    public void checkPermissionsShouldReturnResponseStatusOnException() throws Exception{
        when(copyIssueService.checkFieldPermissions(null)).thenThrow(new ProjectNotFoundException(errors));

        verifyFailure(localJiraProxy.checkPermissions(null), errors);
    }

    @Test
    public void testCopyLocalLinkOutwardWhenRemoteTypeChanges() throws Exception{
        IssueLinkType linkType = mock(IssueLinkType.class);
        MockIssue localIssue = new MockIssue();
        localIssue.setId(1L);

        localJiraProxy.copyLocalIssueLink(localIssue, null, 2L, linkType, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        //changing remote link direction should not affect
        localJiraProxy.copyLocalIssueLink(localIssue, null, 2L, linkType, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        localJiraProxy.copyLocalIssueLink(localIssue, null, 2L, linkType, JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.IGNORE);

        verify(issueLinkManager, times(3)).createIssueLink(1L, 2L, 0L, null, currentUser);
    }

    @Test
    public void testCopyLocalLinkInward() throws Exception{
        IssueLinkType linkType = mock(IssueLinkType.class);
        MockIssue localIssue = new MockIssue();
        localIssue.setId(1L);

        verifySuccess(localJiraProxy.copyLocalIssueLink(localIssue, null, 2L, linkType, JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD));

        verify(issueLinkManager).createIssueLink(2L, 1L, 0L, null, currentUser);
    }

    private static class RemoteIssueMatcherByRefs implements ArgumentMatcher<RemoteIssueLink> {

        private final Long id;
        private final Long issueId;
        private final String globalId;

        private RemoteIssueMatcherByRefs(Long id, Long issueId, String globalId) {
            this.id = id;
            this.issueId = issueId;
            this.globalId = globalId;
        }

        @Override
        public boolean matches(RemoteIssueLink link) {
            if( link == null)
                return false;

            return Objects.equals(id, link.getId())
                    && Objects.equals(issueId, link.getIssueId())
                    && Objects.equals(globalId, link.getGlobalId());
        }
    }

    @Test
    public void testCopyRemoteIssueLinkAndCreateException() throws CreateException {
        final String globalId = "globalId";


        MockIssue issue = new MockIssue();
        issue.setId(123L);
        RemoteIssueLink sourceLink = new RemoteIssueLinkBuilder().globalId(globalId).build();
        when(issueManager.getIssueObject("key")).thenReturn(issue);

        verifySuccess(localJiraProxy.copyRemoteIssueLink(sourceLink, "key"));

        verify(remoteIssueLinkManager).createRemoteIssueLink(argThat(new RemoteIssueMatcherByRefs(null, issue.getId(), globalId)), eq(currentApplicationUser));

        //testing wrapping exception into response
        reset(remoteIssueLinkManager);
        when(remoteIssueLinkManager.createRemoteIssueLink(any(RemoteIssueLink.class), eq(currentApplicationUser))).thenThrow(new CreateException("message"));
        verifyFailure(localJiraProxy.copyRemoteIssueLink(sourceLink, "key"));
    }

    @Test
    public void convertRemoteIssueLinksIntoLocalShouldAlwaysBeSuccessful(){
        verifySuccess(localJiraProxy.convertRemoteIssueLinksIntoLocal(null));
    }

    @Test
    public void testGetIssueUrl(){
        when(jiraBaseUrls.baseUrl()).thenReturn("myBase");

        String result = localJiraProxy.getIssueUrl("myIssue");

        assertEquals("myBase/browse/myIssue", result);
    }

    private NegativeResponseStatus verifyFailure(Either<NegativeResponseStatus, ?> either){
        return EitherTestingUtils.verifyFailure(either, localJiraProxy.getJiraLocation());
    }

    private NegativeResponseStatus verifyFailure(Either<NegativeResponseStatus, ?> either, ErrorCollection errors){
        return EitherTestingUtils.verifyFailure(either, errors, localJiraProxy.getJiraLocation());
    }

    private SuccessfulResponse verifySuccess(Either<NegativeResponseStatus, SuccessfulResponse> either){
        return EitherTestingUtils.verifySuccess(either, localJiraProxy.getJiraLocation());
    }

    private <T extends ResultWithJiraLocation<?>> T extractResult(Either<NegativeResponseStatus, T > either){
        return EitherTestingUtils.extractResult(either, localJiraProxy.getJiraLocation());
    }

    private <T> T extractRight(Either<NegativeResponseStatus, T > either){
        return EitherTestingUtils.extractRight(either);
    }

}
