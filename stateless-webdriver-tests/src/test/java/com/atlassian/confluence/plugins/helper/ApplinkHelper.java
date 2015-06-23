package com.atlassian.confluence.plugins.helper;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraWebDriverTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;

public class ApplinkHelper
{
    private static final String TEST_APPLINK_NAME = "jiratest";
    public static enum ApplinkMode { BASIC, OAUTH, TRUSTED }

    public static String setupAppLink(ApplinkMode applinkMode, HttpClient client, String authArgs, String basicAuthArgs) throws IOException, JSONException
    {
        String applinkId = null;
        if(!isExistAppLink(client, authArgs))
        {
            applinkId = createAppLink(client, TEST_APPLINK_NAME, authArgs, AbstractJiraWebDriverTest.JIRA_BASE_URL, AbstractJiraWebDriverTest.JIRA_DISPLAY_URL, true);

            switch (applinkMode)
            {
                case BASIC:
                    enableApplinkBasicMode(client, applinkId, basicAuthArgs);
                    break;
                case OAUTH:
                    enableApplinkOauthMode(client, applinkId, authArgs);
                    break;
                case TRUSTED:
                    enableApplinkTrustedMode(client, applinkId, authArgs);
            }
        }
        return applinkId;
    }

    public static boolean isExistAppLink(HttpClient client, String authArgs) throws JSONException, IOException
    {
        final JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i = 0; i< jsonArray.length(); i++)
        {
            final String url = jsonArray.getJSONObject(i).getString("rpcUrl");
            Assert.assertNotNull(url);
            if (url.equals(AbstractJiraWebDriverTest.JIRA_BASE_URL))
            {
                return true;
            }
        }
        return false;
    }

    public static JSONArray getListAppLink(HttpClient client, String authArgs) throws IOException, JSONException
    {
        final GetMethod m = new GetMethod(System.getProperty("baseurl.confluence") + "/rest/applinks/1.0/applicationlink" + authArgs);
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);
        String responseBody = m.getResponseBodyAsString();
        Assert.assertTrue("Response should be a json object : "+ responseBody, responseBody.startsWith("{"));

        final JSONObject jsonObj = new JSONObject(responseBody);
        return jsonObj.getJSONArray("applicationLinks");
    }

    public static String createAppLink(HttpClient client, String applinkName, String authArgs, String rpcURL, String displayURL, boolean isPrimary) throws IOException, JSONException
    {
        final PostMethod m = new PostMethod(System.getProperty("baseurl.confluence") + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"" + applinkName + "\",\"rpcUrl\":\"" + rpcURL + "\",\"displayUrl\":\"" + displayURL + "\",\"isPrimary\":" + isPrimary + "},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }

    public static void enableApplinkBasicMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final PutMethod method = new PutMethod(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/basic/" + applinkId + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Basic AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_MOVED_TEMPORARILY);

    }

    public static void enableApplinkOauthMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final PostMethod method = new PostMethod(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + applinkId + authArgs);
        method.addParameter("outgoing-enabled", "true");
        method.addRequestHeader("X-Atlassian-Token", "no-check");

        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Oauth AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }

    public static void enableApplinkTrustedMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        PostMethod method = new PostMethod(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + applinkId + authArgs);
        method.addParameter("action", "ENABLE");
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }
}
