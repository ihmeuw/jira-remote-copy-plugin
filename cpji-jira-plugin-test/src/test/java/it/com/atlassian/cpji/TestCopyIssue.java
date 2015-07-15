package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.CopyDetailsPage;
import com.atlassian.cpji.tests.pageobjects.CopyIssueToInstanceConfirmationPage;
import com.atlassian.cpji.tests.pageobjects.CopyIssueToInstanceSuccessfulPage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.cpji.tests.rules.CreateIssues;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.security.Permissions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import scala.Unit;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static com.atlassian.cpji.tests.RawRestUtil.getIssueJson;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

/**
 * @since v2.1
 */
public class TestCopyIssue extends AbstractCopyIssueTest {
    private static final String JIRA2_DATE_PICKER_CF = "customfield_10001";
    private static final String JIRA2_GROUP_PICKER_CF = "customfield_10002";
    private static final String JIRA2_MULTI_GROUP_PICKER_CF = "customfield_10004";
    private static final String JIRA2_FREE_TEXT_FIELD_CF = "customfield_10000";
    private static final String JIRA2_SELECT_LIST_CF = "customfield_10006";
    private static final String JIRA2_NUMBER_FIELD_CF = "customfield_10005";

    @Rule
    public CreateIssues createIssues = new CreateIssues(restClient1);

    @Before
    public void setUp() {
        login(jira1);
    }

    @Test
    public void testWithCustomFields() throws Exception {
        final String remoteIssueKey = remoteCopy(jira1, "TST-1", 10000L);

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("CLONE - A test bug", fields.getString("summary"));
        assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"));
        assertEquals("Blah blah blah", fields.getString("description"));

        // Custom fields
        assertEquals("2011-09-30", fields.getString(JIRA2_DATE_PICKER_CF));
        assertEquals("jira-developers", fields.getJSONObject(JIRA2_GROUP_PICKER_CF).getString("name"));

        final JSONArray multiGroup = fields.getJSONArray(JIRA2_MULTI_GROUP_PICKER_CF);
        assertEquals("jira-developers", multiGroup.getJSONObject(0).getString("name"));
        assertEquals("jira-users", multiGroup.getJSONObject(1).getString("name"));

        assertEquals("Free text field.", fields.getString(JIRA2_FREE_TEXT_FIELD_CF));
        assertEquals("beta", fields.getJSONObject(JIRA2_SELECT_LIST_CF).getString("value"));
        assertEquals(12345.679, fields.getDouble(JIRA2_NUMBER_FIELD_CF), 0);
    }

    @Test
    public void testAttachmentsCopying() throws Exception {
        final String remoteIssueKey = remoteCopy(jira1, "NEL-4", 10400L);

        Issue restIssue = restClient2.getIssueClient().getIssue(remoteIssueKey, ImmutableList.of(IssueRestClient.Expandos.CHANGELOG)).claim();
        List<Attachment> attachments = ImmutableList.copyOf(restIssue.getAttachments());
        checkAttachment(attachments.get(0), "document.doc", 9216, "application/msword");
        checkAttachment(attachments.get(1), "screenshot.png", 36743, "image/png");

        //after copying attachments, change history should be empty
        assertEquals("Change history should be empty as we clear it", 0, Iterables.size(restIssue.getChangelog()));
    }

    private void checkAttachment(Attachment attachment, String filename, int size, String mimeType) throws Exception {
        assertEquals(filename, attachment.getFilename());
        assertEquals(size, attachment.getSize());
        assertEquals(mimeType, attachment.getMimeType());
    }

    @Test
    public void testWithoutCustomFields() throws Exception {
        final String remoteIssueKey = remoteCopy(jira1, "TST-2", 10100L, "Blah", "A test task with changed name");

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("A test task with changed name", fields.getString("summary"));
        assertEquals("Task", fields.getJSONObject("issuetype").getString("name"));
        assertEquals(JSONObject.NULL, fields.opt("description"));

        // Custom fields
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_DATE_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_MULTI_GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_FREE_TEXT_FIELD_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_SELECT_LIST_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_NUMBER_FIELD_CF));
    }

    private String remoteCopy(final JiraTestedProduct jira, final String issueKey, final Long issueId) {
        return remoteCopy(jira, issueKey, issueId, "Blah", null);
    }

    private String remoteCopy(final JiraTestedProduct jira, final String issueKey, final Long issueId, final String destinationProject, final String summary) {
        return remoteCopy(jira, issueKey, issueId, destinationProject, summary, copyIssueToInstanceConfirmationPage -> {
            Poller.waitUntilTrue(copyIssueToInstanceConfirmationPage.areAllIssueFieldsRetained());
            Poller.waitUntilTrue(copyIssueToInstanceConfirmationPage.areAllRequiredFieldsFilledIn());
        });
    }

    private String remoteCopy(final JiraTestedProduct jira, final String issueKey, final Long issueId, final String destinationProject, final String summary, final Consumer<CopyIssueToInstanceConfirmationPage> assertions) {
        viewIssue(jira, issueKey);
        SelectTargetProjectPage selectTargetProjectPage = jira.visit(SelectTargetProjectPage.class, issueId);
        if (destinationProject != null)
            selectTargetProjectPage.setDestinationProject(destinationProject);

        final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
        if(summary != null)
            copyDetailsPage.enterNewSummary(summary);

        final CopyIssueToInstanceConfirmationPage copyIssueToInstanceConfirmationPage = copyDetailsPage.next();

        assertions.accept(copyIssueToInstanceConfirmationPage);

        final CopyIssueToInstanceSuccessfulPage copyIssueToInstanceSuccessfulPage = copyIssueToInstanceConfirmationPage.copyIssue();
        assertTrue(copyIssueToInstanceSuccessfulPage.isSuccessful());

        return copyIssueToInstanceSuccessfulPage.getRemoteIssueKey();
    }

    @Test
    public void testCopyForProjectWithoutProjectEntityLinks() throws IOException, JSONException {
        final String remoteIssueKey = remoteCopy(jira1, "NEL-3", 10301L, "Destination not entity links", null);
        assertThat(remoteIssueKey, startsWith("DNEL"));

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("CLONE - Testing as admin", fields.getString("summary"));
        assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"));
        assertEquals(JSONObject.NULL, fields.opt("description"));

        // Custom fields
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_DATE_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_MULTI_GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_FREE_TEXT_FIELD_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_SELECT_LIST_CF));
        assertEquals(JSONObject.NULL, fields.opt(JIRA2_NUMBER_FIELD_CF));
    }

    @Test
    public void testCopyWithBrowseProjectsPermissionDisabled() throws Exception {

        String remoteIssueKey = null;

        try {
            testkit2.permissionSchemes().removeProjectRolePermission(0L, Permissions.BROWSE, 10000L);
            remoteIssueKey = remoteCopy(jira1, "NEL-3", 10301L, "Destination not entity links", null);
            assertThat(remoteIssueKey, startsWith("DNEL"));
        } finally {
            testkit2.permissionSchemes().addProjectRolePermission(0L, Permissions.BROWSE, 10000L);
        }

        assertNotNull(remoteIssueKey);
        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("CLONE - Testing as admin", fields.getString("summary"));
        assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"));
    }

    @Test
    public void testCopyIssueAssociatedWithInvalidUser() throws Exception {
        final com.atlassian.jira.testkit.client.restclient.Issue issue = testkit1.issues().getIssue("IWRU-1");

        final String remoteIssueKey = remoteCopy(jira1, issue.key, Long.parseLong(issue.id), "Blah", "A test task with changed name", copyIssueToInstanceConfirmationPage -> {
            Poller.waitUntilTrue(copyIssueToInstanceConfirmationPage.areAllIssueFieldsRetained());
        });

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("A test task with changed name", fields.getString("summary"));
        assertEquals("admin", fields.getJSONObject("assignee").getString("name"));
    }
}
