package it.com.atlassian.cpji;

import com.atlassian.cpji.tests.pageobjects.CopyDetailsPage;
import com.atlassian.cpji.tests.pageobjects.CopyIssueToInstancePage;
import com.atlassian.cpji.tests.pageobjects.PermissionChecksPage;
import com.atlassian.cpji.tests.pageobjects.SelectTargetProjectPage;
import com.atlassian.cpji.tests.rules.CreateIssues;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.IssueFieldId;
import com.atlassian.jira.rest.client.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.atlassian.jira.rest.client.domain.IssueFieldId.PROJECT_FIELD;
import static com.atlassian.jira.rest.client.domain.IssueFieldId.SUMMARY_FIELD;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @since v2.1
 */
public class TestCopyIssue extends AbstractCopyIssueTest
{
    private static final String DATE_PICKER_CF = "customfield_10001";
    private static final String GROUP_PICKER_CF = "customfield_10002";
    private static final String MULTI_GROUP_PICKER_CF = "customfield_10004";
    private static final String FREE_TEXT_FIELD_CF = "customfield_10000";
    private static final String SELECT_LIST_CF = "customfield_10006";
    private static final String NUMBER_FIELD_CF = "customfield_10005";

	@Rule
	public CreateIssues createIssues = new CreateIssues(restClient1);

    @Before
    public void setUp()
    {
        login(jira1);
    }

    @Test
    public void testWithCustomFields() throws Exception
    {
        final String remoteIssueKey = remoteCopy(jira1, "TST-1", 10000L);

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("A test bug", fields.getString("summary"));
        assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"));
        assertEquals("Blah blah blah", fields.getString("description"));

        // Custom fields
        assertEquals("2011-09-30", fields.getString(DATE_PICKER_CF));
        assertEquals("jira-developers", fields.getJSONObject(GROUP_PICKER_CF).getString("name"));

        final JSONArray multiGroup = fields.getJSONArray(MULTI_GROUP_PICKER_CF);
        assertEquals("jira-developers", multiGroup.getJSONObject(0).getString("name"));
        assertEquals("jira-users", multiGroup.getJSONObject(1).getString("name"));

        assertEquals("Free text field.", fields.getString(FREE_TEXT_FIELD_CF));
        assertEquals("beta", fields.getJSONObject(SELECT_LIST_CF).getString("value"));
        assertEquals(12345.679, fields.getDouble(NUMBER_FIELD_CF), 0);
    }

    @Test
    public void testAttachmentsCopying() throws Exception{
        final String remoteIssueKey = remoteCopy(jira1, "NEL-4", 10400L);

        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");
        final JSONArray attachments = fields.getJSONArray("attachment");

        final JSONObject document = attachments.getJSONObject(0);
        checkAttachment(document, "document.doc", 9216, "application/msword; charset=ISO-8859-1");

        final JSONObject screenshot = attachments.getJSONObject(1);
        checkAttachment(screenshot, "screenshot.png", 36743, "image/png; charset=ISO-8859-1");

    }

    private void checkAttachment(JSONObject attachment, String filename, int size, String mimeType) throws Exception{
        assertEquals(filename, attachment.getString("filename"));
        assertEquals(size, attachment.getInt("size"));
        assertEquals(mimeType, attachment.getString("mimeType"));
    }

    @Test
    public void testWithoutCustomFields() throws Exception
    {
        final String remoteIssueKey = remoteCopy(jira1, "TST-2", 10100L);

        // Query the remotely copied issue via REST
        final JSONObject json = getIssueJson(jira2, remoteIssueKey);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals("A test task", fields.getString("summary"));
        assertEquals("Task", fields.getJSONObject("issuetype").getString("name"));
        assertEquals(JSONObject.NULL, fields.opt("description"));

        // Custom fields
        assertEquals(JSONObject.NULL, fields.opt(DATE_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(MULTI_GROUP_PICKER_CF));
        assertEquals(JSONObject.NULL, fields.opt(FREE_TEXT_FIELD_CF));
        assertEquals(JSONObject.NULL, fields.opt(SELECT_LIST_CF));
        assertEquals(JSONObject.NULL, fields.opt(NUMBER_FIELD_CF));
    }

    private String remoteCopy(final JiraTestedProduct jira, final String issueKey, final Long issueId)
    {
        viewIssue(jira, issueKey);
        SelectTargetProjectPage selectTargetProjectPage = jira.visit(SelectTargetProjectPage.class, issueId);

        final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
        final PermissionChecksPage permissionChecksPage = copyDetailsPage.next();

        assertTrue(permissionChecksPage.isAllSystemFieldsRetained());
        assertTrue(permissionChecksPage.isAllCustomFieldsRetained());

        final CopyIssueToInstancePage copyIssueToInstancePage = permissionChecksPage.copyIssue();
        assertTrue(copyIssueToInstancePage.isSuccessful());

        final String remoteIssueKey = copyIssueToInstancePage.getRemoteIssueKey();
        return remoteIssueKey;
    }

    private JSONObject getIssueJson(final JiraTestedProduct jira, final String remoteIssueKey) throws IOException, JSONException
    {
        final String restUrl = jira.getProductInstance().getBaseUrl() + "/rest/api/2/issue/" + remoteIssueKey + "?os_username=admin&os_password=admin";
        final HttpClient httpClient = new DefaultHttpClient();
        final HttpGet httpGet = new HttpGet(restUrl);
        final HttpResponse response = httpClient.execute(httpGet);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String entity = EntityUtils.toString(response.getEntity());
        return new JSONObject(entity);
    }

	@Test
	public void testCopyForProjectWithoutProjectEntityLinks() throws IOException, JSONException {
		viewIssue(jira1, "NEL-3");
		SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, 10301L);

		selectTargetProjectPage.setDestinationProject("Destination not entity links");

		final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
		final PermissionChecksPage permissionChecksPage = copyDetailsPage.next();

		assertTrue(permissionChecksPage.isAllSystemFieldsRetained());
		assertTrue(permissionChecksPage.isAllCustomFieldsRetained());

		final CopyIssueToInstancePage copyIssueToInstancePage = permissionChecksPage.copyIssue();
		assertTrue(copyIssueToInstancePage.isSuccessful());

		final String remoteIssueKey = copyIssueToInstancePage.getRemoteIssueKey();
		assertThat(remoteIssueKey, startsWith("DNEL"));

		// Query the remotely copied issue via REST
		final JSONObject json = getIssueJson(jira2, remoteIssueKey);
		final JSONObject fields = json.getJSONObject("fields");

		// System fields
		assertEquals("Testing as admin", fields.getString("summary"));
		assertEquals("Bug", fields.getJSONObject("issuetype").getString("name"));
		assertEquals(JSONObject.NULL, fields.opt("description"));

		// Custom fields
		assertEquals(JSONObject.NULL, fields.opt(DATE_PICKER_CF));
		assertEquals(JSONObject.NULL, fields.opt(GROUP_PICKER_CF));
		assertEquals(JSONObject.NULL, fields.opt(MULTI_GROUP_PICKER_CF));
		assertEquals(JSONObject.NULL, fields.opt(FREE_TEXT_FIELD_CF));
		assertEquals(JSONObject.NULL, fields.opt(SELECT_LIST_CF));
		assertEquals(JSONObject.NULL, fields.opt(NUMBER_FIELD_CF));
	}

	@Test
	public void testIssueWithComments() {
		Issue issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
				new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.with("key", "TST")),
				new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.with("id", "3")));

		restClient1.getIssueClient().addComment(NPM, issue.getCommentsUri(), new Comment(null,
				"This is a comment", null, null, new DateTime(), new DateTime(), null, null));
	}
}
