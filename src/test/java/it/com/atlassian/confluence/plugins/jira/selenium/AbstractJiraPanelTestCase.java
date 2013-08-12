package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;
import com.thoughtworks.selenium.Wait;

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
        client.click("jiralink");
        assertThat.textPresentByTimeout("Insert JIRA Issue", 5000);
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
                
        Wait wait = new Wait("Checking Jira link") {
            public boolean until() {
                return client.isElementPresent("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
            }
        };
        wait.wait("Couldn't find new Jira link", 5000);
        
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        
        String attributeValue = client
                .getAttribute("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']/@data-macro-parameters");

        client.selectFrame("relative=top");        
        return attributeValue;
    }
}
