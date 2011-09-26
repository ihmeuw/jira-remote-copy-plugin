package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import it.com.atlassian.cpji.pages.CopyDetailsPage;
import it.com.atlassian.cpji.pages.CopyIssueToInstancePage;
import it.com.atlassian.cpji.pages.PermissionChecksPage;
import it.com.atlassian.cpji.pages.SelectTargetProjectPage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since v2.1
 */
public class TestCopyIssue extends AbstractCopyIssueTest
{
    private static final String ISSUE_KEY = "TST-1";
    private static final Long ISSUE_ID = 10000L;
    private static final String ISSUE_SUMMARY = "A test bug";
    private static final String ISSUE_TYPE = "Bug";
    private static final String ISSUE_DESCRIPTION = "Blah blah blah";


    @Before
    public void setUp()
    {
        login(jira1);
    }

    @Test
    public void testSimpleRemoteCopy() throws Exception
    {
        viewIssue(jira1, ISSUE_KEY);
        SelectTargetProjectPage selectTargetProjectPage = jira1.visit(SelectTargetProjectPage.class, ISSUE_ID);
        
        final CopyDetailsPage copyDetailsPage = selectTargetProjectPage.next();
        final PermissionChecksPage permissionChecksPage = copyDetailsPage.next();

        assertTrue(permissionChecksPage.isAllSystemFieldsRetained());
        assertTrue(permissionChecksPage.isAllCustomFieldsRetained());

        final CopyIssueToInstancePage copyIssueToInstancePage = permissionChecksPage.copyIssue();
        assertTrue(copyIssueToInstancePage.isSuccessful());

        final String remoteIssueKey = copyIssueToInstancePage.getRemoteIssueKey();

        // Query the remotely copied issue via REST
        final String restUrl = jira2.getProductInstance().getBaseUrl() + "/rest/api/2/issue/" + remoteIssueKey + "?os_username=admin&os_password=admin";
        final JSONObject json = httpGetJson(restUrl);
        final JSONObject fields = json.getJSONObject("fields");

        // System fields
        assertEquals(ISSUE_SUMMARY, fields.getString("summary"));
        assertEquals(ISSUE_TYPE, fields.getJSONObject("issuetype").getString("name"));
        assertEquals(ISSUE_DESCRIPTION, fields.getString("description"));

        // Custom fields
        // DatePickerCF
        assertEquals("2011-09-30", fields.getString("customfield_10001"));

        // GroupPickerCF
        assertEquals("jira-developers", fields.getString("customfield_10002"));

        // MultiGroupPickerCF
        final JSONArray multiGroup = fields.getJSONArray("customfield_10004");
        assertEquals("jira-developers", multiGroup.getString(0));
        assertEquals("jira-users", multiGroup.getString(1));

        // FreeTextFieldCF
        assertEquals("Free text field.", fields.getString("customfield_10000"));

        // SelectListCF
        assertEquals("beta", fields.getJSONObject("customfield_10006").getString("value"));

        // NumberFieldCF
        assertEquals(12345.679, fields.getDouble("customfield_10005"), 0);
    }

    private JSONObject httpGetJson(final String url) throws IOException, JSONException
    {
        final HttpClient httpClient = new DefaultHttpClient();
        final HttpGet httpGet = new HttpGet(url);
        final HttpResponse response = httpClient.execute(httpGet);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String entity = EntityUtils.toString(response.getEntity());
        return new JSONObject(entity);
    }

    private void login(final JiraTestedProduct jiraTestedProduct)
    {
        jiraTestedProduct.gotoLoginPage().loginAsSysAdmin(DashboardPage.class);
    }

    private ViewIssuePage viewIssue(final JiraTestedProduct jiraTestedProduct, final String issueKey)
    {
        return jiraTestedProduct.getPageBinder().navigateToAndBind(ViewIssuePage.class, issueKey);
    }
}
