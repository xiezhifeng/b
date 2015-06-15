package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.user.impl.DefaultUser;
import com.google.common.collect.ImmutableSet;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

final public class ApplinkHelper
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String CONFLUENCE_BASE_URL = WebDriverConfiguration.getBaseUrl();
    private static final String APPLINK_WS =  "/rest/applinks/1.0/applicationlink";
    private static final String CREATE_APPLINK_WS = "/rest/applinks/1.0/applicationlinkForm/createAppLink";
    private static final String HTTP_QUERY_PARAM_ACCEPT_TYPE = "application/json, text/javascript, */*";
    private static final String OUTGOING_APPLINK_ACTIVATE = "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/";
    private static final String INCOMING_APPLINK_ACTIVATE = "/plugins/servlet/applinks/auth/conf/oauth/add-consumer-by-url/";
    private static final String TEST_APPLINK_NAME = "jiratest";
    public static enum ApplinkMode { BASIC, OAUTH, TRUSTED }

    private ApplinkHelper()
    {

    }

    public static void removeAllApplink() throws JSONException, IOException
    {
        removeAllApplink(CONFLUENCE_BASE_URL);
        removeAllApplink(JIRA_BASE_URL);
    }
    /**
     * Remove all applinks
     * @throws JSONException
     * @throws IOException
     */
    public static void removeAllApplink(String baseUrl) throws JSONException, IOException
    {
        JSONArray jsonArray = getListAppLink(baseUrl);
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++)
        {
            final String id = jsonArray.getJSONObject(i).getString("id");
            ids.add(id);
        }

        for (String id : ids)
        {
            CloseableHttpResponse res = RestTestHelper.deleteRestResponse(RestTestHelper.getDefaultUser(), baseUrl + APPLINK_WS + "/" + id);
            Assert.assertEquals(200, res.getStatusLine().getStatusCode());
        }
    }

    /**
     * Setup applink
     * @param applinkMode
     * @return applink id
     * @throws IOException
     * @throws JSONException
     */
    public static String setupAppLink(ApplinkMode applinkMode) throws IOException, JSONException
    {
        String applinkId = null;
        if(!isExistAppLink(CONFLUENCE_BASE_URL))
        {
            switch (applinkMode)
            {
                case BASIC:
                    applinkId = createAppLink(TEST_APPLINK_NAME, AbstractJiraWebDriverTest.JIRA_BASE_URL, AbstractJiraWebDriverTest.JIRA_DISPLAY_URL, true, true);
                    enableApplinkBasicMode(applinkId);
                    break;
                case OAUTH:
                    applinkId = createAppLink(TEST_APPLINK_NAME, AbstractJiraWebDriverTest.JIRA_BASE_URL, AbstractJiraWebDriverTest.JIRA_DISPLAY_URL, true, true);
                    enableApplinkOauthMode(applinkId);
                    break;
                case TRUSTED:
                    applinkId = createTrustedAppLink();
            }
        }
        return applinkId;
    }

    /**
     * setup open authentication mode for applink
     * @param applinkId
     * @throws IOException
     */
    public static void enableApplinkOauthMode(String applinkId) throws IOException
    {
        HttpClient client = getWebsudoHttpClient();
        String url = CONFLUENCE_BASE_URL + OUTGOING_APPLINK_ACTIVATE + applinkId + getAuthQueryString();
        final PostMethod method = new PostMethod(url);
        method.addParameter("outgoing-enabled", "true");
        method.addRequestHeader("X-Atlassian-Token", "no-check");

        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Oauth AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }


    private static HttpClient getWebsudoHttpClient() throws IOException
    {
        HttpClient client = new HttpClient();
        doWebSudo(client);
        return client;
    }
    /**
     * setup basic mode for applink
     * @param applinkId
     * @throws IOException
     */
    public static void enableApplinkBasicMode(String applinkId) throws IOException
    {
        HttpClient client = new HttpClient();
        final PostMethod method = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/basic/" + applinkId + getAuthQueryString());
        final int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_MOVED_TEMPORARILY);
    }

    /**
     * setup trusted mode for applink
     * @param applinkId
     * @throws IOException
     */
    public static void enableApplinkTrustedMode(String applinkId) throws IOException
    {
        HttpClient client = getWebsudoHttpClient();
        PostMethod method = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + applinkId + getAuthQueryString());
        method.addParameter("action", "ENABLE");
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(method);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + method.getResponseBodyAsString(), status == HttpStatus.SC_OK);
    }


    /**
     * Create a new applink
     * @param applinkName
     * @return applink id
     * @throws IOException
     * @throws JSONException
     */
    public static String createAppLink(String applinkName, String rpcURL, String displayURL, boolean isPrimary, boolean isTwoWay) throws IOException, JSONException {
        final String reqBody = buildCreateApplinkJson(applinkName, rpcURL, displayURL, RestTestHelper.getDefaultUser(), isPrimary, false, false, isTwoWay).toString();
        final CloseableHttpResponse response = RestTestHelper.postRestResponse(RestTestHelper.getDefaultUser(), CONFLUENCE_BASE_URL + CREATE_APPLINK_WS, reqBody);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        final JSONObject jsonObj = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }

    private static JSONObject buildCreateApplinkJson(String name, String remoteUrl, String displayUrl, DefaultUser user, boolean primary, boolean trustEachOther,
                                                     boolean sharedUserBase, boolean createTwoWayLink) throws JSONException {
        JSONObject inputAppLink = new JSONObject();
        inputAppLink.put("id", UUID.randomUUID());
        inputAppLink.put("typeId", "jira");
        inputAppLink.put("name", name);
        inputAppLink.put("rpcUrl", remoteUrl);
        inputAppLink.put("displayUrl", displayUrl);
        inputAppLink.put("isPrimary", primary);

        JSONObject configFormValuesInput = new JSONObject();
        configFormValuesInput.put("trustEachOther", trustEachOther);
        configFormValuesInput.put("shareUserbase", sharedUserBase);

        JSONObject input = new JSONObject();
        input.put("applicationLink", inputAppLink);
        input.put("username", user.getName());
        input.put("password", user.getPassword());
        input.put("createTwoWayLink", createTwoWayLink);
        input.put("customRpcURL", false);
        input.put("rpcUrl", remoteUrl);
        input.put("configFormValues", configFormValuesInput);
        return input;
    }

    /**
     * Delete applink
     * @param client
     * @param applinkId
     * @param authArgs
     * @throws IOException
     */
    public static void deleteApplink(HttpClient client, String applinkId, String authArgs) throws IOException
    {
        final DeleteMethod method = new DeleteMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/2.0/applicationlink/" + applinkId + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(method);
        Assert.assertEquals(HttpStatus.SC_OK, status);
    }

    /**
     * Get primary applink id
     * @return applink id
     */
    public static String getPrimaryApplinkId()
    {
        try
        {
            JSONArray jsonArray = getListAppLink(CONFLUENCE_BASE_URL);
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

    /**
     * Get list applink
     * @return JSONArray
     * @throws IOException
     * @throws JSONException
     */
    private static JSONArray getListAppLink(String baseUrl) throws IOException, JSONException
    {
        CloseableHttpResponse response = RestTestHelper.getRestResponse(RestTestHelper.getDefaultUser(), baseUrl + APPLINK_WS);
        String result= IOUtils.toString(response.getEntity().getContent(), "UTF-8");;
        final JSONObject jsonObj = new JSONObject(result);
        JSONArray jsonArray = jsonObj.getJSONArray("applicationLinks");
        return jsonArray;
    }

    /**
     * Check applink which is exist or notß
     * @return true/false
     * @throws JSONException
     * @throws IOException
     */
    public static boolean isExistAppLink(String baseUrl) throws JSONException, IOException
    {
        return getListAppLink(baseUrl).length() > 0;
    }

    public static String createTrustedAppLink() throws IOException, JSONException {
        String url = WebDriverConfiguration.getBaseUrl() + CREATE_APPLINK_WS;
        boolean isPrimary = true;
        boolean trustEachOther = true;
        boolean sharedUserBase = true;
        JSONObject applinkConfiguration = buildCreateApplinkJson(TEST_APPLINK_NAME, AbstractJiraWebDriverTest.JIRA_BASE_URL, AbstractJiraWebDriverTest.JIRA_DISPLAY_URL, RestTestHelper.getDefaultUser(),
                isPrimary, trustEachOther, sharedUserBase, true);
        final CloseableHttpResponse manifestResponse = RestTestHelper.postRestResponse(RestTestHelper.getDefaultUser(), url, applinkConfiguration.toString());
        assertThat(manifestResponse.getStatusLine().getStatusCode(), is(200));
        return applinkConfiguration.getJSONObject("applicationLink").getString("id");
    }

    public static String getAuthQueryString()
    {
        return "?os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    public static void doWebSudo(final HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/confluence/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        assertThat("WebSudo auth returned unexpected status", ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK), hasItem(status));
    }
}
