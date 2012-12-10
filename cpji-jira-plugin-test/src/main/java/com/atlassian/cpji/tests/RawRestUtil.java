package com.atlassian.cpji.tests;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @since v3.0
 */
public class RawRestUtil {

	public static JSONObject getIssueJson(final JiraTestedProduct jira, final String remoteIssueKey) throws IOException,
			JSONException
	{
		final String restUrl = jira.getProductInstance().getBaseUrl() + "/rest/api/2/issue/" + remoteIssueKey + "?os_username=admin&os_password=admin";
		final HttpClient httpClient = new DefaultHttpClient();
		final HttpGet httpGet = new HttpGet(restUrl);
		final HttpResponse response = httpClient.execute(httpGet);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String entity = EntityUtils.toString(response.getEntity());
		return new JSONObject(entity);
	}

	public static JSONArray getIssueRemoteLinksJson(final JiraTestedProduct jira, final String remoteIssueKey) throws IOException,
			JSONException
	{
		final String restUrl = jira.getProductInstance().getBaseUrl() + "/rest/api/2/issue/" + remoteIssueKey + "/remotelink?os_username=admin&os_password=admin";
		final HttpClient httpClient = new DefaultHttpClient();
		final HttpGet httpGet = new HttpGet(restUrl);
		final HttpResponse response = httpClient.execute(httpGet);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String entity = EntityUtils.toString(response.getEntity());
		return new JSONArray(entity);
	}

}
