package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;

public class ApplinkHelper
{
    public static enum ApplinkMode { BASIC, OAUTH, TRUSTED }

    public static void removeAllAppLink(HttpClient client, String authArgs) throws JSONException, InvalidOperationException, IOException
    {
        JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i=0; i < jsonArray.length(); i++)
        {
            String applinkId = jsonArray.getJSONObject(i).getString("id");
            deleteApplink(client, applinkId, authArgs);
        }
    }

    public static String setupAppLink(ApplinkMode applinkMode, HttpClient client, String authArgs) throws IOException, JSONException
    {
        String idAppLink = null;
        if(!isExistAppLink(client, authArgs))
        {
            idAppLink = createAppLink(client, "jiratest", authArgs);

            switch (applinkMode)
            {
                case BASIC:
                    enableApplinkBasicMode(client, idAppLink, authArgs);
                    break;
                case OAUTH:
                    enableApplinkOauthMode(client, idAppLink, authArgs);
                    break;
                case TRUSTED:
                    enableApplinkTrustedMode(client, idAppLink, authArgs);
            }
        }
        return idAppLink;
    }

    public static void enableApplinkOauthMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final PostMethod method = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + applinkId + authArgs);
        method.addParameter("outgoing-enabled", "true");
        method.addRequestHeader("X-Atlassian-Token", "no-check");

        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Oauth AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }

    public static void enableApplinkBasicMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final PutMethod method = new PutMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/basic/" + applinkId + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_MOVED_TEMPORARILY);
    }

    public static void enableApplinkTrustedMode(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        PostMethod method = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + applinkId + authArgs);
        method.addParameter("action", "ENABLE");
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }


    public static String createAppLink(HttpClient client, String applinkName, String authArgs) throws IOException, JSONException
    {
        final PostMethod m = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"" + applinkName + "\",\"rpcUrl\":\"" + AbstractJiraWebDriverTest.JIRA_BASE_URL + "\",\"displayUrl\":\"" + AbstractJiraWebDriverTest.JIRA_DISPLAY_URL + "\",\"isPrimary\":true},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }

    public static void deleteApplink(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final DeleteMethod method = new DeleteMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/2.0/applicationlink/" + applinkId + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(method);
        Assert.assertEquals(HttpStatus.SC_OK, status);
    }

    /**
     * @return primary applink id, or null if empty or error occurs
     */
    public static String getPrimaryApplinkId(HttpClient client, String authArgs)
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

    public static JSONArray getListAppLink(HttpClient client, String authArgs) throws IOException, JSONException
    {
        final GetMethod m = new GetMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/1.0/applicationlink" + authArgs);
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);
        String responseBody = m.getResponseBodyAsString();
        Assert.assertTrue("Response should be a json object : "+ responseBody, responseBody.startsWith("{"));

        final JSONObject jsonObj = new JSONObject(responseBody);
        return jsonObj.getJSONArray("applicationLinks");
    }

    private static boolean isExistAppLink(HttpClient client, String authArgs) throws JSONException, IOException
    {
        final JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i = 0; i< jsonArray.length(); i++)
        {
            final String url = jsonArray.getJSONObject(i).getString("rpcUrl");
            Assert.assertNotNull(url);
            if(url.equals(AbstractJiraWebDriverTest.JIRA_BASE_URL))
            {
                return true;
            }
        }
        return false;
    }
}
