package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.openqa.selenium.UnhandledAlertException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AbstractJiraWebDriverTest extends AbstractWebDriverTest
{
    private static final Logger LOGGER = Logger.getLogger(JiraChartWebDriverTest.class);
    protected String jiraBaseUrl = System.getProperty("baseurl.jira1", "http://localhost:11990/jira");
    protected String jiraDisplayUrl = jiraBaseUrl.replace("localhost", "127.0.0.1");
    
    private static final String APPLINK_WS = "/rest/applinks/1.0/applicationlink";
    
    @Override
    public void start() throws Exception
    {
        try
        {
            super.start();

        } catch (UnhandledAlertException ex)
        {
            LOGGER.warn("Unexpected alert was opened");
        }

        setupAppLink(true);
    }

    protected void setupAppLink(boolean isBasicMode) throws IOException, JSONException
    {
        String authArgs = getAuthQueryString();
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        if(!checkExistAppLink(client, authArgs))
        {
            final String idAppLink = createAppLink(client, authArgs);
            if(isBasicMode)
            {
                enableApplinkBasicMode(client, getBasicQueryString(), idAppLink);
            }
            else
            {
                enableOauthWithApplink(client, authArgs, idAppLink);
            }
        }
    }
    
    protected void setupTrustedAppLink()  throws IOException, JSONException{
        String authArgs = getAuthQueryString();
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        if(!checkExistAppLink(client, authArgs))
        {
            final String idAppLink = createAppLink(client, authArgs);
            enableApplinkTrustedApp(client, getBasicQueryString(), idAppLink);
        }
    }
    
    private String getAuthQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }

    private String getBasicQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }
    
    private boolean checkExistAppLink(HttpClient client, String authArgs) throws JSONException, HttpException, IOException
    {
        final JSONArray jsonArray = getListAppLink(client, authArgs);
        for(int i = 0; i< jsonArray.length(); i++)
        {
            final String url = jsonArray.getJSONObject(i).getString("rpcUrl");
            Assert.assertNotNull(url);
            if(url.equals(jiraBaseUrl))
            {
                return true;
            }
        }
        return false;
    }

    private JSONArray getListAppLink(HttpClient client, String authArgs) throws HttpException, IOException, JSONException
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
    
    private void doWebSudo(HttpClient client) throws IOException, HttpException
    {
        final PostMethod l = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/confluence/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        Assert.assertTrue(status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK);
    }

    private String createAppLink(HttpClient client, String authArgs) throws HttpException, IOException, JSONException
    {
        final PostMethod m = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"testjira\",\"rpcUrl\":\"" + jiraBaseUrl + "\",\"displayUrl\":\"" + jiraDisplayUrl + "\",\"isPrimary\":true},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }
    
    protected void removeAllAppLink() throws JSONException, InvalidOperationException, HttpException, IOException
    {
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        
        WebResource webResource = null;
        javax.ws.rs.core.MultivaluedMap<String, String> queryParams = new com.sun.jersey.core.util.MultivaluedMapImpl();
        queryParams.add("os_username", User.ADMIN.getUsername());
        queryParams.add("os_password", User.ADMIN.getPassword());
        
        List<String> ids  = new ArrayList<String>();
        
        Client clientJersey = Client.create();
        webResource = clientJersey.resource(WebDriverConfiguration.getBaseUrl() + APPLINK_WS);

        String result = webResource.queryParams(queryParams).accept("application/json, text/javascript, */*").get(String.class);
        final JSONObject jsonObj = new JSONObject(result);
        JSONArray jsonArray = jsonObj.getJSONArray("applicationLinks");
        for(int i = 0; i< jsonArray.length(); i++) {
            final String id = jsonArray.getJSONObject(i).getString("id");
            ids.add(id);
        }
        
        //delete all server config in applink
        for(String id: ids)
        {
            String response = webResource.path(id).queryParams(queryParams).accept("application/json, text/javascript, */*").delete(String.class);
            final JSONObject deleteResponse = new JSONObject(response);
            int status = deleteResponse.getInt("status-code");
            if (status != 200){
                throw new InvalidOperationException("Cannot delete applink");
            }
        }
    }

    private void enableOauthWithApplink(HttpClient client, String authArgs, String idAppLink) throws HttpException, IOException
    {
        final PostMethod setTrustMethod = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + idAppLink + authArgs);
        setTrustMethod.addParameter("outgoing-enabled", "true");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");

        final int status = client.executeMethod(setTrustMethod);
        Assert.assertEquals(HttpStatus.SC_OK, status);
    }

    private void enableApplinkBasicMode(HttpClient client, String authArgs, String idAppLink) throws IOException
    {
        final PutMethod method = new PutMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/basic/" + idAppLink + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        final int status = client.executeMethod(method);
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, status);
    }
    
    private void enableApplinkTrustedApp(HttpClient client, String authArgs, String idAppLink) throws HttpException, IOException
    {
        PostMethod setTrustMethod = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + idAppLink + authArgs);
        setTrustMethod.addParameter("action", "ENABLE");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(setTrustMethod);
        Assert.assertTrue("Cannot enable Trusted AppLink", status == 200);;
    }

    public void waitForMacroOnEditor(final EditContentPage editContentPage, final String macroName)
    {
        Poller.waitUntilTrue("Macro did not appear in edit page",
                new TimedQuery<Boolean>() {

                    @Override
                    public long interval()
                    {
                        return 100;
                    }

                    @Override
                    public long defaultTimeout()
                    {
                        return 10000;
                    }

                    @Override
                    public Boolean byDefaultTimeout()
                    {
                        return hasMacro();
                    }

                    @Override
                    public Boolean by(long timeoutInMillis)
                    {
                        return hasMacro();
                    }

                    @Override
                    public Boolean by(long timeout, TimeUnit unit)
                    {
                        return hasMacro();
                    }

                    @Override
                    public Boolean now()
                    {
                        return hasMacro();
                    }

                    private boolean hasMacro()
                    {
                        return editContentPage.getContent().getHtml().contains("data-macro-name=\"" + macroName + "\"");
                    }

                });
    }

    protected EditContentPage createJiraIssue(JiraCreatedMacroDialog jiraMacroDialog, String project,
                                            String issueType, String summary, String epicName)
    {
        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject(project);
        jiraMacroDialog.selectIssueType(issueType);
        jiraMacroDialog.setSummary(summary);
        if(epicName != null)
        {
            jiraMacroDialog.setEpicName(epicName);
        }
        EditContentPage editContentPage = jiraMacroDialog.insertIssue();
        waitForMacroOnEditor(editContentPage, "jira");
        return editContentPage;
    }
}
