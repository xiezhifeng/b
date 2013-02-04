package it.com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;
import com.atlassian.confluence.plugin.functest.helper.PageHelper;
import com.atlassian.confluence.plugin.functest.helper.SpaceHelper;
import it.com.atlassian.confluence.extra.jira.JiraIssuesMacroTestCase.JiraIssue;
import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;


public class AbstractJiraMacrosPluginTestCase extends AbstractConfluencePluginWebTestCase
{
    private static final String URL_TYPE_XML = "jira.issueviews:searchrequest-xml";

    protected static final String URL_TYPE_RSS_ISSUES = "jira.issueviews:searchrequest-rss";

    protected static final String URL_TYPE_RSS_COMMENTS = "jira.issueviews:searchrequest-comments-rss";

    Properties jiraWebTesterProperties;

    protected WebTester jiraWebTester;

    protected String testSpaceKey;

    Properties confluenceBuildInfo;

    protected String jiraBaseUrl = System.getProperty("baseurl.jira1", "http://localhost:11990/jira");
    protected String jiraDisplayUrl = jiraBaseUrl.replace("localhost", "127.0.0.1");

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        initConfluenceBuildInfo();
       // initJiraWebTesterConfig();
        setupJiraWebTester();
        loginToJira("admin", "admin");
        //restoreJiraData("jira-func-tests-data.xml");

        createTestSpace();
    }

    String getContextPath()
    {
        return getElementAttributByXPath("//meta[@id='confluence-context-path']", "content");
    }

    private void initConfluenceBuildInfo() throws IOException
    {
        InputStream in = null;

        try
        {
            confluenceBuildInfo = new Properties();
            in = getClass().getClassLoader().getResourceAsStream("com/atlassian/confluence/default.properties");

            confluenceBuildInfo.load(in);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private void createTestSpace()
    {
        SpaceHelper spaceHelper = getSpaceHelper();
        spaceHelper.setKey(testSpaceKey = "tst");
        spaceHelper.setName("Test Space");
        spaceHelper.setDescription("Test Space");

        assertTrue(spaceHelper.create());
    }

//    private Properties initJiraWebTesterConfig() throws IOException
//    {
//        InputStream in = getClass().getClassLoader().getResourceAsStream("jira-webtester.properties");
//
//        try
//        {
//            jiraWebTesterProperties = new Properties();
//            jiraWebTesterProperties.load(in);
//            return jiraWebTesterProperties;
//
//        }
//        finally
//        {
//            IOUtils.closeQuietly(in);
//        }
//    }

    protected String getJiraWebTesterConfig(String property)
    {
        return jiraWebTesterProperties.getProperty(property);
    }

    private void setupJiraWebTester() throws IOException
    {
        jiraWebTester = new WebTester();
        jiraWebTester.setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT);
        jiraWebTester.setScriptingEnabled(false);
        jiraWebTester.getTestContext().setBaseUrl(jiraBaseUrl);

        jiraWebTester.beginAt("/");
    }

    protected void loginToJira(String userName, String password)
    {
        jiraWebTester.gotoPage("/login.jsp");
        jiraWebTester.setWorkingForm("loginform");
        jiraWebTester.setTextField("os_username", userName);
        jiraWebTester.setTextField("os_password", password);
        jiraWebTester.submit();

        assertLinkPresentWithText("Log Out");
    }

    private File copyClassPathResourceToFile(String classPathResource) throws IOException
    {
        InputStream in = null;
        OutputStream out = null;

        try
        {
            File tempFile = File.createTempFile("it.com.atlassian.confluence.extra.jira", null);

            in = getClass().getClassLoader().getResourceAsStream(classPathResource);
            out = new BufferedOutputStream(new FileOutputStream(tempFile));

            IOUtils.copy(in, out);

            return tempFile;
        }
        finally
        {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

//    protected void restoreJiraData(String classPathJiraBackupXml)
//    {
//        jiraWebTester.clickLink("admin_link");
//        jiraWebTester.clickLink("restore_data");
//
//        try
//        {
//            File jiraBackupXml = copyClassPathResourceToFile(classPathJiraBackupXml);
//
//            jiraWebTester.setWorkingForm("jiraform");
//            jiraWebTester.setTextField("filename", jiraBackupXml.getAbsolutePath());
//            jiraWebTester.submit("Restore");
//
//            loginToJira("admin", "admin");
//        }
//        catch (IOException ioe)
//        {
//            fail("Unable to copy " + classPathJiraBackupXml + " to a temp file.\n" + ExceptionUtils.getFullStackTrace(ioe));
//        }
//    }

    protected void logoutFromJira()
    {
        clickLinkWithText("Log Out");
    }

    protected void trustConfluenceApplication()
    {
        jiraWebTester.clickLink("admin_link");
        jiraWebTester.clickLink("trusted_apps");

        jiraWebTester.setWorkingForm("jiraform");
        jiraWebTester.setTextField("trustedAppBaseUrl", getConfluenceWebTester().getBaseUrl());
        jiraWebTester.submit("Send Request");

        jiraWebTester.setWorkingForm("jiraform");
        jiraWebTester.submit("Add");
    }
    protected void createConfluenceOauthConsumerInJira()
    {
        jiraWebTester.clickLink("admin_link");
        jiraWebTester.clickLink("oauth");
        jiraWebTester.clickLinkWithExactText("Add OAuth Consumer");

        jiraWebTester.setWorkingForm("add-by-url");
        jiraWebTester.setTextField("baseUrl", getConfluenceWebTester().getBaseUrl());
        jiraWebTester.submit();
    }

    protected String setupAppLink() throws HttpException, IOException, JSONException
    {
        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);

        HttpClient client = new HttpClient();
        String baseUrl = ((JWebUnitConfluenceWebTester)tester).getBaseUrl();
        String jiraUrl = jiraWebTester.getTestContext().getBaseUrl().toString();

        if (jiraUrl.endsWith("/"))
        {
            jiraUrl = jiraUrl.substring(0, jiraUrl.length() - 1);
        }

        doWebSudo(adminUserName, adminPassword, client, baseUrl);

        PostMethod m = new PostMethod(baseUrl + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"testjira\",\"rpcUrl\":\"" + jiraUrl + "\",\"displayUrl\":\"" + jiraDisplayUrl + "\",\"isPrimary\":false},\"username\":\"\",\"password\":\"\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        int status = client.executeMethod(m);
        assertEquals(200, status);

        JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        String id = jsonObj.getJSONObject("applicationLink").getString("id");
        return id;

    }

    protected void enableTrustedAuthWithAppLink(String id) throws HttpException, IOException
    {
        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);

        String baseUrl = ((JWebUnitConfluenceWebTester)tester).getBaseUrl();
        HttpClient client = new HttpClient();

        doWebSudo(adminUserName, adminPassword, client, baseUrl);

        PostMethod setTrustMethod = new PostMethod(baseUrl + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + id + authArgs);
        setTrustMethod.addParameter("action", "ENABLE");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(setTrustMethod);

        assertEquals(200, status);
    }

    private String getAuthQueryString(String adminUserName, String adminPassword)
    {
        String authArgs = "?os_username=" + adminUserName + "&os_password=" + adminPassword;
        return authArgs;
    }

    private void doWebSudo(String adminUserName, String adminPassword,
            HttpClient client, String baseUrl) throws IOException,
            HttpException
    {
        String authArgs = getAuthQueryString(adminUserName, adminPassword);
        PostMethod l = new PostMethod(baseUrl + "/confluence/doauthenticate.action" + authArgs);
        l.addParameter("password", adminPassword);
        int status = client.executeMethod(l);
        assertEquals(302, status);
    }

    protected void enableOauthWithApplink(String id) throws HttpException, IOException
    {
        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);

        String baseUrl = ((JWebUnitConfluenceWebTester)tester).getBaseUrl();
        HttpClient client = new HttpClient();

        doWebSudo(adminUserName, adminPassword, client, baseUrl);

        PostMethod setTrustMethod = new PostMethod(baseUrl + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/" + id + authArgs);
        setTrustMethod.addParameter("outgoing-enabled", "true");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(setTrustMethod);

        assertEquals(200, status);
    }
    protected void untrustConfluenceApplication()
    {
        jiraWebTester.clickLink("admin_link");
        jiraWebTester.clickLink("trusted_apps");

        jiraWebTester.clickLinkWithText("Delete");
    }

    protected long createPage(String testSpacekey, String pageTitle, String wikiMarkup)
    {
        PageHelper pageHelper = getPageHelper();

        pageHelper.setSpaceKey(testSpacekey);
        pageHelper.setTitle(pageTitle);
        pageHelper.setContent(wikiMarkup);

        assertTrue(pageHelper.create());

        return pageHelper.getId();
    }

    protected void viewPageById(long pageId)
    {
        gotoPage("/pages/viewpage.action?pageId=" + pageId);
    }

    int getConflenceBuildNumber()
    {
        return Integer.parseInt(confluenceBuildInfo.getProperty("build.number"));
    }

    protected String getJiraIssuesXmlUrl()
    {
        return getJiraIssuesXmlUrl(10000, 1000);
    }

    protected String getJiraIssuesXmlUrl(int projectId)
    {
        return getJiraIssuesXmlUrl(projectId, 1000);
    }

    protected String getJiraIssuesXmlUrl(int projectId, int resultsPerPage)
    {
        return getJiraIssuesXmlUrl(projectId, URL_TYPE_XML, resultsPerPage);
    }

    protected String getJiraIssuesXmlUrl(int projectId, String type, int resultsPerPage)
    {
        return new StringBuffer(jiraWebTester.getTestContext().getBaseUrl().toString())
                .append("sr/").append(type).append("/temp/SearchRequest.xml?pid=" + projectId + "&sorter/field=issuekey&sorter/order=DESC&tempMax=").append(resultsPerPage)
                .toString();
    }

    protected String getIssueRetrieverUrl(int page, int resultsPerPage)
    {
        return new StringBuffer(getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']//input[@name='retrieverUrlHtml']", "value").substring(getContextPath().length()))
                .append("&page=").append(page)
                .append("&rp=").append(resultsPerPage)
                .toString();
    }

    protected void assertJiraIssues(int page, int total, List<JiraIssue> jiraIssues,
            String json, boolean fromApplink) throws JSONException
    {
        JSONObject jsonObject = new JSONObject(json);

        assertEquals(page, jsonObject.get("page"));
        assertEquals(total, jsonObject.get("total"));

        JSONArray jsonArray = jsonObject.getJSONArray("rows");

        assertEquals(jiraIssues.size(), null == jsonArray ? 0 : jsonArray.length());

        if (null != jsonArray)
        {
            for (int i = 0; i < jsonArray.length(); ++i)
            {
                JSONObject jiraIssueInJson = jsonArray.getJSONObject(i);
                JiraIssue jiraIssue = jiraIssues.get(i);

                assertEquals(jiraIssue.key, jiraIssueInJson.get("id"));

                JSONArray jiraIssueCellsJson = jiraIssueInJson.getJSONArray("cell");
                String jiraBaseUrl = jiraWebTester.getTestContext().getBaseUrl().toString();

                /* Take of ending forward slash */
                jiraBaseUrl = jiraBaseUrl.substring(0, jiraBaseUrl.length() - 1);

                if (fromApplink)
                {
                    jiraBaseUrl = jiraDisplayUrl;
                }


                assertEquals("iconSource",
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" ><img src=\"" + jiraBaseUrl + jiraIssue.iconSource + "\" alt=\"" + jiraIssue.iconAltText + "\"/></a>",
                        jiraIssueCellsJson.get(0)
                );
                assertEquals("Key",
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" >" + jiraIssue.key + "</a>",
                        jiraIssueCellsJson.get(1)
                );
                assertEquals("Summary",
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" >" + jiraIssue.summary + "</a>",
                        jiraIssueCellsJson.get(2)
                );
                assertEquals("Assignee",
                        jiraIssue.assignee,
                        jiraIssueCellsJson.get(3)
                );
                assertEquals("Reporter",
                        jiraIssue.reporter,
                        jiraIssueCellsJson.get(4)
                );
                assertEquals("priorityIcon",
                        "<img src=\"" + jiraBaseUrl + jiraIssue.priorityIcon + "\" alt=\"" + jiraIssue.priorityAltText + "\"/>",
                        jiraIssueCellsJson.get(5)
                );
                assertEquals("statusIcon",
                        "<img src=\"" + jiraBaseUrl + jiraIssue.statusIcon + "\" alt=\"" + jiraIssue.statusAltText + "\"/> " + jiraIssue.statusAltText,
                        jiraIssueCellsJson.get(6)
                );
                assertEquals("Resolution",
                        jiraIssue.resolution,
                        jiraIssueCellsJson.get(7)
                );
                assertEquals("CreatedDate",
                        jiraIssue.createdDate,
                        jiraIssueCellsJson.get(8)
                );
                assertEquals("LastUpdatedDate",
                        jiraIssue.lastUpdatedDate,
                        jiraIssueCellsJson.get(9)
                );
                assertEquals("DueDate",
                        StringUtils.defaultString(jiraIssue.dueDate),
                        jiraIssueCellsJson.get(10)
                );
            }
        }
    }
    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            untrustConfluenceApplication();
        }
        catch(Throwable t){}
        super.tearDown();
    }
}
