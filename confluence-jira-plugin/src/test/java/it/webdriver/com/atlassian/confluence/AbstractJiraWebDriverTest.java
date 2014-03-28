package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Group;
import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.IssuesControl;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;
import com.atlassian.pageobjects.binder.PageBindingException;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.jiracharts.JiraChartWebDriverTest;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;

public abstract class AbstractJiraWebDriverTest extends AbstractWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

    private static final Pattern JIRA_ISSUE_MACRO_PARAMETERS_PATTERN = Pattern.compile("<img\\s+.*data-macro-name=\"jira\"\\s+.*data-macro-parameters=\"([^\"]+)\"");

    public static final String JIRA_ISSUE_MACRO_NAME = "jira";

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraChartWebDriverTest.class);
    
    protected String jiraDisplayUrl = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    protected String authArgs;
    protected final HttpClient client = new HttpClient();
    private static final String APPLINK_WS = "/rest/applinks/1.0/applicationlink";
    private static final int RETRY_TIME = 8;

    protected Map<String, JiraProjectModel> jiraProjects = new HashMap<String, JiraProjectModel>();
    protected EditContentPage editContentPage;

    protected static Backdoor testKitJIRA;

    @BeforeClass
    public static void prepareGlobal()
    {
        testKitJIRA = new Backdoor(new TestKitLocalEnvironmentData());
    }
    
    @Before
    public void setup() throws Exception
    {
        authArgs = getAuthQueryString();
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode()) {
            // Need to set up applinks if not running against an OD instance
            removeAllAppLink();
            setupTrustedAppLink();
        }
        else
        {
            // Addition configuration which needs to be done for OD instances:
            //    1. Setting up proper user permissions
            //    2. Initialising the SOAP service for JIRA project creation
            initForOnDemand();
        }

        editContentPage = product.loginAndEdit(User.ADMIN, Page.TEST);
    }

    private void initForOnDemand() throws Exception
    {
        // Hack - set correct user group while UserManagementHelper is still being fixed (CONFDEV-20880). This logic should be handled by using Group.USERS
        Group userGroup = TestProperties.isOnDemandMode() ? Group.ONDEMAND_ALACARTE_USERS : Group.CONF_ADMINS;

        // Setup User.ADMIN to have all permissions
        userHelper.createGroup(Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, userGroup);

        userHelper.synchronise();
        // Hack - the synchronise method doesn't actually sync the directory on OD so we just need to wait... Should also be addressed in CONFDEV-20880
        Thread.sleep(10000);

        JiraRestHelper.initJiraSoapServices();
    }

    @After
    public void tearDown() throws Exception
    {
        // Determine whether or not we are still inside the editor by checking if the RTE 'Cancel' button is present
        if (editContentPage != null && editContentPage.getEditor().isCancelVisiableNow())
        {
            editContentPage.cancel();
        }

        Iterator<JiraProjectModel> projectIterator = jiraProjects.values().iterator();
        while (projectIterator.hasNext())
        {
            JiraRestHelper.deleteJiraProject(projectIterator.next().getProjectKey(), client);
        }

        serverStateManager.removeTestData();
    }

    protected String setupAppLink(boolean isBasicMode) throws IOException, JSONException
    {
        String idAppLink = null;
        if(!checkExistAppLink())
        {
            idAppLink = createAppLink();
            if(isBasicMode)
            {
                enableApplinkBasicMode(getBasicQueryString(), idAppLink);
            }
            else
            {
                enableOauthWithApplink(idAppLink);
            }
        }
        return idAppLink;
    }

    protected MacroBrowserDialog openMacroBrowser()
    {
        MacroBrowserDialog macroBrowserDialog = null;
        int retry = 1;
        PageBindingException ex = null;
        while (macroBrowserDialog == null && retry <= RETRY_TIME)
        {
            try
            {
                macroBrowserDialog = editContentPage.openMacroBrowser();
            }
            catch (PageBindingException e)
            {
                ex = e;
            }
            LOGGER.warn("Couldn't bind MacroBrower, retrying {} time", retry);
            retry++;
        }

        if (macroBrowserDialog == null && ex != null)
        {
            throw ex;
        }

        Poller.waitUntil(macroBrowserDialog.isVisibleTimed(), is(true), Poller.by(15, TimeUnit.SECONDS));
        return macroBrowserDialog;
    }

    protected void setupTrustedAppLink() throws IOException, JSONException
    {
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        if (!checkExistAppLink())
        {
            final String idAppLink = createAppLink();
            enableApplinkTrustedApp(client, getAuthQueryString(), idAppLink);
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

    private boolean checkExistAppLink() throws JSONException, IOException
    {
        final JSONArray jsonArray = getListAppLink();
        for(int i = 0; i< jsonArray.length(); i++)
        {
            final String url = jsonArray.getJSONObject(i).getString("rpcUrl");
            Assert.assertNotNull(url);
            if(url.equals(JIRA_BASE_URL))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return primary applink id, or null if empty or error occurs
     */
    protected String getPrimaryApplinkId() 
    {
        try
        {
            JSONArray jsonArray = getListAppLink();
            for(int i = 0; i< jsonArray.length(); i++)
            {
                if (jsonArray.getJSONObject(i).getBoolean("isPrimary")) {
                    return jsonArray.getJSONObject(i).getString("id");
                }
            }
        } catch (Exception e)
        {
            // do nothing
        }
        return null;
    }

    protected JSONArray getListAppLink() throws IOException, JSONException
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
    
    private void doWebSudo(HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/confluence/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        Assert.assertTrue(status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK);
    }

    private String createAppLink() throws IOException, JSONException
    {
        return createAppLink("testjira");
    }

    private String createAppLink(String applinkName) throws IOException, JSONException
    {
        final PostMethod m = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);
        
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"" + applinkName + "\",\"rpcUrl\":\"" + JIRA_BASE_URL + "\",\"displayUrl\":\"" + jiraDisplayUrl + "\",\"isPrimary\":true},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);
        
        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);
        
        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }
    
    protected void removeAllAppLink() throws JSONException, InvalidOperationException, IOException
    {
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        
        javax.ws.rs.core.MultivaluedMap<String, String> queryParams = new com.sun.jersey.core.util.MultivaluedMapImpl();
        queryParams.add("os_username", User.ADMIN.getUsername());
        queryParams.add("os_password", User.ADMIN.getPassword());
        
        List<String> ids  = new ArrayList<String>();
        
        Client clientJersey = Client.create();
        WebResource webResource = clientJersey.resource(WebDriverConfiguration.getBaseUrl() + APPLINK_WS);

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

    private void enableOauthWithApplink(String idAppLink) throws IOException
    {
        final PostMethod setTrustMethod = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + idAppLink + authArgs);
        setTrustMethod.addParameter("outgoing-enabled", "true");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");

        final int status = client.executeMethod(setTrustMethod);
        Assert.assertEquals(HttpStatus.SC_OK, status);
    }

    private void enableApplinkBasicMode(String authArgs, String idAppLink) throws IOException
    {
        final PutMethod method = new PutMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/basic/" + idAppLink + authArgs);
        method.addRequestHeader("X-Atlassian-Token", "no-check");
        final int status = client.executeMethod(method);
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, status);
    }
    
    private void enableApplinkTrustedApp(HttpClient client, String authArgs, String idAppLink) throws IOException
    {
        PostMethod setTrustMethod = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + idAppLink + authArgs);
        setTrustMethod.addParameter("action", "ENABLE");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(setTrustMethod);
        Assert.assertTrue("Cannot enable Trusted AppLink. " + setTrustMethod.getResponseBodyAsString(), status == 200);
    }

    public void waitUntilInlineMacroAppearsInEditor(final EditContentPage editContentPage, final String macroName)
    {
        Poller.waitUntil(
                "Macro could not be found on editor page",
                editContentPage.getContent().getRenderedContent().hasInlineMacro(macroName, Collections.EMPTY_LIST),
                is(true),
                Poller.by(10, TimeUnit.SECONDS)
        );
    }

    @SuppressWarnings("deprecation")
    protected void waitForAjaxRequest(final AtlassianWebDriver webDriver)
    {
        webDriver.waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(@Nullable WebDriver input)
            {
                return (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;");
            }
        });
    }

    protected Map<String, String> getJiraIssueParams(EditContentPage editingPage)
    {
        String content = editingPage.getEditor().getContent().getTimedHtml().byDefaultTimeout();

        Matcher paramMatcher = JIRA_ISSUE_MACRO_PARAMETERS_PATTERN.matcher(content);
        if (!paramMatcher.find())
            return null;

        String parameterString = paramMatcher.group(1);
        return getParameters(parameterString);
    }

    protected void validateJiraIssueFields(JiraIssueBean issueBean)
    {
        Issue issue = getIssues().getIssue(issueBean.getKey());
        Assert.assertEquals(issue.fields.summary, issueBean.getSummary());
        Assert.assertEquals(issue.fields.description, issueBean.getDescription());
        Assert.assertEquals(issue.fields.project.id, issueBean.getProjectId());
        Assert.assertEquals(issue.fields.issuetype.id, issueBean.getIssueTypeId());
        // will get user config in applink as default reporter
        Assert.assertEquals(issue.fields.reporter.name, "admin");
    }

    protected IssuesControl getIssues()
    {
        return testKitJIRA.issues();
    }

    private static Map<String, String> getParameters(String macroParamString)
    {
        Map<String, String> params = new HashMap<String, String>();
        StringTokenizer tokenizer = new StringTokenizer(macroParamString, "|");
        while (tokenizer.hasMoreTokens())
        {
            String paramDefinition = tokenizer.nextToken();
            StringTokenizer definitionTokenizer = new StringTokenizer(paramDefinition, "=");

            if (definitionTokenizer.hasMoreTokens())
            {
                final String paramName = definitionTokenizer.nextToken();
                final String paramValue;

                if (!definitionTokenizer.hasMoreTokens())
                    paramValue = StringUtils.EMPTY;
                else
                    paramValue = definitionTokenizer.nextToken();
                params.put(paramName, paramValue);
            }
            else
            {
                throw new IllegalArgumentException("Bad macro parameter string: " + macroParamString);
            }
        }

        return params;
    }
}
