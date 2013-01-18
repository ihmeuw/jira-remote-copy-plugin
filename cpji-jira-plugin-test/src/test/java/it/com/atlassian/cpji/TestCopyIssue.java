package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.CopyDetailsPage;
import com.atlassian.cpji.tests.pageobjects.CopyIssueToInstanceConfirmationPage;
import com.atlassian.cpji.tests.pageobjects.CopyIssueToInstanceSuccessfulPage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.cpji.tests.rules.CreateIssues;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.security.Permissions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.atlassian.cpji.tests.RawRestUtil.getIssueJson;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

    private static final String JIRA1_DATE_PICKER_CF = "customfield_10000";
    private static final String JIRA1_GROUP_PICKER_CF = "customfield_10001";
    private static final String JIRA1_MULTI_GROUP_PICKER_CF = "customfield_10002";
    private static final String JIRA1_FREE_TEXT_FIELD_CF = "customfield_10005";
    private static final String JIRA1_SELECT_LIST_CF = "customfield_10004";
    private static final String JIRA1_NUMBER_FIELD_CF = "customfield_10003";

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

        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");
        final JSONArray attachments = fields.getJSONArray("attachment");

        final JSONObject document = attachments.getJSONObject(0);
        checkAttachment(document, "document.doc", 9216, "application/msword; charset=ISO-8859-1");

        final JSONObject screenshot = attachments.getJSONObject(1);
        checkAttachment(screenshot, "screenshot.png", 36743, "image/png; charset=ISO-8859-1");

    }

    private void checkAttachment(JSONObject attachment, String filename, int size, String mimeType) throws Exception {
        assertEquals(filename, attachment.getString("filename"));
        assertEquals(size, attachment.getInt("size"));
        assertEquals(mimeType, attachment.getString("mimeType"));
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
        viewIssue(jira, issueKey);
        SelectTargetProjectPage selectTargetProjectPage = jira.visit(SelectTargetProjectPage.class, issueId);
        if (destinationProject != null)
            selectTargetProjectPage.setDestinationProject(destinationProject);

        final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
        if(summary != null)
            copyDetailsPage.enterNewSummary(summary);

        final CopyIssueToInstanceConfirmationPage copyIssueToInstanceConfirmationPage = copyDetailsPage.next();

        assertTrue(copyIssueToInstanceConfirmationPage.isAllSystemFieldsRetained());
        assertTrue(copyIssueToInstanceConfirmationPage.isAllCustomFieldsRetained());

        final CopyIssueToInstanceSuccessfulPage copyIssueToInstanceSuccessfulPage = copyIssueToInstanceConfirmationPage.copyIssue();
        assertTrue(copyIssueToInstanceSuccessfulPage.isSuccessful());

        final String remoteIssueKey = copyIssueToInstanceSuccessfulPage.getRemoteIssueKey();
        return remoteIssueKey;
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

}
