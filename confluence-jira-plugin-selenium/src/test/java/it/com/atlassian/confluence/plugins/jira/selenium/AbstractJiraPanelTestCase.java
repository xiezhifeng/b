package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;

public class AbstractJiraPanelTestCase extends AbstractJiraDialogTestCase
{
    
    private static final Logger LOG = Logger.getLogger(AbstractJiraPanelTestCase.class);
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        login();

        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
    }

    protected void openJiraDialog()
    {
        LOG.debug("openJiraDialog");
        assertThat.elementPresentByTimeout("jiralink", 10000);
        if (requireApplink()) {
            client.waitForCondition("window.AJS.Editor.JiraConnector.servers && window.AJS.Editor.JiraConnector.servers.length > 0", 5000);
        }
        client.click("jiralink");
        assertThat.textPresentByTimeout("Insert JIRA Issue", 5000);
    }
    
    protected boolean requireApplink() {
        return true;
    }
    
    protected String addJiraAppLink(String name, String url, String displayUrl,
            Boolean isPrimary) throws HttpException, IOException, JSONException {
        final String adminUserName = getConfluenceWebTester()
                .getAdminUserName();
        final String adminPassword = getConfluenceWebTester()
                .getAdminPassword();
        final String authArgs = getAuthQueryString(adminUserName, adminPassword);

        final HttpClient client = new HttpClient();
        final String baseUrl = ((JWebUnitConfluenceWebTester) tester)
                .getBaseUrl();

        final PostMethod m = new PostMethod(baseUrl
                + "/rest/applinks/1.0/applicationlinkForm/createAppLink"
                + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        // add new Jira server with set primary for selected default
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\""
                + name
                + "\",\"rpcUrl\":\""
                + url
                + "\",\"displayUrl\":\""
                + displayUrl
                + "\",\"isPrimary\":"
                + isPrimary.toString()
                + "},\"username\":\"\",\"password\":\"\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,
                "application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        assertEquals(200, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        final String id = jsonObj.getJSONObject("applicationLink").getString(
                "id");
        return id;
    }

    protected void enableOauthWithApplink(String id) throws HttpException,
            IOException {
        final String adminUserName = getConfluenceWebTester()
                .getAdminUserName();
        final String adminPassword = getConfluenceWebTester()
                .getAdminPassword();
        final String authArgs = getAuthQueryString(adminUserName, adminPassword);

        final String baseUrl = ((JWebUnitConfluenceWebTester) tester)
                .getBaseUrl();
        final HttpClient client = new HttpClient();

        final PostMethod setTrustMethod = new PostMethod(
                baseUrl
                        + "/plugins/servlet/applinks/auth/conf/oauth/outbound/atlassian/"
                        + id + authArgs);
        setTrustMethod.addParameter("outgoing-enabled", "true");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        final int status = client.executeMethod(setTrustMethod);

        assertEquals(200, status);
    }
    
    /**
     * validate param in data-macro-parameters from the macro placeholder in the
     * Editor
     * 
     * @param paramMarco
     */
    protected void validateParamInLinkMacro(String paramMarco) {
        String parameters = getJiraMacroParameters();
        assertTrue(parameters.contains(paramMarco));
    }

    /**
     * 
     * @return the value of the data-macro-parameters attribute from the macro
     *         placeholder in the Editor. Only the first found macro is used.
     */
    protected String getJiraMacroParameters() {
        // look macro link in RTE
        client.selectFrame("wysiwygTextarea_ifr");
        
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jira']", 5000);
        
        String attributeValue = client
                .getAttribute("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']/@data-macro-parameters");

        client.selectFrame("relative=top");        
        return attributeValue;
    }
}
