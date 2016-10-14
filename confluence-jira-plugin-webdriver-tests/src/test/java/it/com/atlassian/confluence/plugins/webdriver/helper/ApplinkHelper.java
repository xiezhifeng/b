package it.com.atlassian.confluence.plugins.webdriver.helper;

import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.util.TimeUtils;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApplinkHelper
{
    private static final String TEST_APPLINK_NAME = "jiratest";
    public static enum ApplinkMode { BASIC, OAUTH, TRUSTED }

    public static String setupAppLink(ApplinkMode applinkMode, CloseableHttpClient client, String authArgs, String basicAuthArgs) throws IOException, JSONException
    {
        String applinkId = null;
        if(!isExistAppLink(client, authArgs))
        {
            applinkId = createAppLink(client, TEST_APPLINK_NAME, authArgs, AbstractJiraTest.JIRA_BASE_URL, AbstractJiraTest.JIRA_DISPLAY_URL, true);

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

    public static boolean isExistAppLink(CloseableHttpClient client, String authArgs) throws JSONException, IOException
    {
        final JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i = 0; i< jsonArray.length(); i++)
        {
            final String url = jsonArray.getJSONObject(i).getString("rpcUrl");
            Assert.assertNotNull(url);
            if (url.equals(AbstractJiraTest.JIRA_BASE_URL))
            {
                return true;
            }
        }
        return false;
    }

    private static JSONArray getListAppLink(CloseableHttpClient client, String authArgs) throws IOException, JSONException
    {
        final HttpGet httpGet = new HttpGet(System.getProperty("baseurl.confluence") + "/rest/applinks/1.0/applicationlink" + authArgs);
        httpGet.setHeader("Accept", "application/json, text/javascript, */*");

        int status;
        String responseBody;
        try(CloseableHttpResponse response = client.execute(httpGet)){
            status = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent());
        }
        Assert.assertEquals(HttpStatus.SC_OK, status);
        Assert.assertTrue("Response should be a json object : "+ responseBody, responseBody.startsWith("{"));

        final JSONObject jsonObj = new JSONObject(responseBody);
        return jsonObj.getJSONArray("applicationLinks");
    }

    public static String createAppLink(CloseableHttpClient client, String applinkName, String authArgs, String rpcURL, String displayURL, boolean isPrimary) throws IOException, JSONException
    {
        final HttpPost httpPost = new HttpPost(System.getProperty("baseurl.confluence") + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        httpPost.setHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"" + applinkName + "\",\"rpcUrl\":\"" + rpcURL + "\",\"displayUrl\":\"" + displayURL + "\",\"isPrimary\":" + isPrimary + "},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringEntity reqEntity = new StringEntity(reqBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(reqEntity);

        int status;
        String responseBody;
        try(CloseableHttpResponse response = client.execute(httpPost)){
            status = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        }
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(responseBody);
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }

    public static void enableApplinkBasicMode(CloseableHttpClient client, String applinkId, String authArgs) throws IOException
    {
        final HttpPut method = new HttpPut(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/basic/" + applinkId + authArgs);
        method.setHeader("X-Atlassian-Token", "no-check");
        int status;
        String responseBody;
        try(CloseableHttpResponse response = client.execute(method)){
            status = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        }
        Assert.assertTrue("Cannot enable Basic AppLink. " + responseBody, status == HttpStatus.SC_MOVED_TEMPORARILY);
        TimeUtils.pause(2000L, TimeUnit.MILLISECONDS);
    }

    public static void enableApplinkOauthMode(CloseableHttpClient client, String applinkId, String authArgs) throws IOException
    {
        final HttpPost method = new HttpPost(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + applinkId + authArgs);
        method.setHeader("X-Atlassian-Token", "no-check");
        List<NameValuePair> parameters = new ArrayList<>(1);
        parameters.add(new BasicNameValuePair("outgoing-enabled", "true"));
        method.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));

        int status;
        String responseBody;
        try(CloseableHttpResponse response = client.execute(method)){
            status = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        }
        Assert.assertTrue("Cannot enable Oauth AppLink. " + responseBody, status == HttpStatus.SC_OK);
        TimeUtils.pause(2000L, TimeUnit.MILLISECONDS);
    }

    public static void enableApplinkTrustedMode(CloseableHttpClient client, String applinkId, String authArgs) throws IOException
    {
        HttpPost method = new HttpPost(System.getProperty("baseurl.confluence") + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + applinkId + authArgs);

        method.setHeader("X-Atlassian-Token", "no-check");
        List<NameValuePair> parameters = new ArrayList<>(1);
        parameters.add(new BasicNameValuePair("action", "ENABLE"));
        method.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));

        int status;
        String responseBody;
        try(CloseableHttpResponse response = client.execute(method)){
            status = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        }
        Assert.assertTrue("Cannot enable Trusted AppLink. " + responseBody, status == HttpStatus.SC_OK);
        TimeUtils.pause(2000L, TimeUnit.MILLISECONDS);
    }

    /**
     * Remove all applinks
     * @param client
     * @param authArgs
     * @throws JSONException
     * @throws com.atlassian.confluence.security.InvalidOperationException
     * @throws IOException
     */
    public static void removeAllAppLink(CloseableHttpClient client, String authArgs) throws JSONException, InvalidOperationException, IOException
    {
        JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i=0; i < jsonArray.length(); i++)
        {
            String applinkId = jsonArray.getJSONObject(i).getString("id");
            deleteApplink(client, applinkId, authArgs);
        }
        TimeUtils.pause(2000L, TimeUnit.MILLISECONDS);
    }

    /**
     * Delete applink
     * @param client
     * @param applinkId
     * @param authArgs
     * @throws IOException
     */
    public static void deleteApplink(CloseableHttpClient client, String applinkId, String authArgs) throws IOException
    {
        final HttpDelete method = new HttpDelete(System.getProperty("baseurl.confluence") + "/rest/applinks/2.0/applicationlink/" + applinkId + authArgs);
        method.setHeader("X-Atlassian-Token", "no-check");
        int status;
        try(CloseableHttpResponse response = client.execute(method)){
            status = response.getStatusLine().getStatusCode();
        }
        Assert.assertEquals(HttpStatus.SC_OK, status);
        TimeUtils.pause(2000L, TimeUnit.MILLISECONDS);
    }

    /**
     * Get primary applink id
     * @param client
     * @param authArgs
     * @return applink id
     */
    public static String getPrimaryApplinkId(CloseableHttpClient client, String authArgs)
    {
        try
        {
            JSONArray jsonArray = getListAppLink(client, authArgs);
            for(int i = 0; i< jsonArray.length(); i++)
            {
                if (jsonArray.getJSONObject(i).getBoolean("isPrimary"))
                {
                    return jsonArray.getJSONObject(i).getString("id");
                }
            }
        }
        catch (Exception e)
        {
            // do nothing
        }
        return null;
    }
}
