package it.com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.confluence.plugin.functest.helper.PageHelper;
import com.atlassian.confluence.plugin.functest.helper.SpaceHelper;
import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;


public class AbstractJiraMacrosPluginTestCase extends AbstractConfluencePluginWebTestCase
{
    static final int LASTEST_CONFLUENCE_210_BUILD = 1519;

    Properties jiraWebTesterProperties;

    WebTester jiraWebTester;

    String testSpaceKey;

    Properties confluenceBuildInfo;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        initConfluenceBuildInfo();
        initJiraWebTesterConfig();
        setupJiraWebTester();
        loginToJira("admin", "admin");
        restoreJiraData("jira-func-tests-data.xml");

        createTestSpace();
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

    private Properties initJiraWebTesterConfig() throws IOException
    {
        InputStream in = getClass().getClassLoader().getResourceAsStream("jira-webtester.properties");

        try
        {
            jiraWebTesterProperties = new Properties();
            jiraWebTesterProperties.load(in);
            return jiraWebTesterProperties;

        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    protected String getJiraWebTesterConfig(String property)
    {
        return jiraWebTesterProperties.getProperty(property);
    }

    private void setupJiraWebTester() throws IOException
    {
        jiraWebTester = new WebTester();
        jiraWebTester.setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT);
        jiraWebTester.setScriptingEnabled(false);
        jiraWebTester.getTestContext().setBaseUrl(getJiraWebTesterConfig("jira.baseurl"));

        jiraWebTester.beginAt("/");
    }

    protected void loginToJira(String userName, String password)
    {
        jiraWebTester.gotoPage("/");
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

    protected void restoreJiraData(String classPathJiraBackupXml)  
    {
        jiraWebTester.clickLink("admin_link");
        jiraWebTester.clickLink("restore_data");

        try
        {
            File jiraBackupXml = copyClassPathResourceToFile(classPathJiraBackupXml);

            jiraWebTester.setWorkingForm("jiraform");
            jiraWebTester.setTextField("filename", jiraBackupXml.getAbsolutePath());
            jiraWebTester.submit("Restore");

            loginToJira("admin", "admin");
        }
        catch (IOException ioe)
        {
            fail("Unable to copy " + classPathJiraBackupXml + " to a temp file.\n" + ExceptionUtils.getFullStackTrace(ioe));
        }
    }

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
}
