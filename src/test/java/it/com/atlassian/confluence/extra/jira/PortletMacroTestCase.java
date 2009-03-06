package it.com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.plugin.functest.ConfluenceWebTester;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class PortletMacroTestCase extends AbstractJiraMacrosPluginTestCase
{
    public void testRenderPortletUntrusted()
    {
        long testPageId = createPage(testSpaceKey, "testRenderPortletUntrusted",
                "{jiraportlet:anonymous=true|url=" + jiraWebTester.getTestContext().getBaseUrl() + "secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:projectstats&projectid=10000&showclosed=false&sortDirection=asc&sortOrder=natural&statistictype=allFixfor}");

        viewPageById(testPageId);

        String jiraPortletHtmlSource = getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraportlet']//iframe", "src")
                .substring(getConfluenceWebTester().getContextPath().length());

        gotoPage(jiraPortletHtmlSource);

        assertEquals("Total Issues: 1 Statistics: Test Project (Fix For Versions (all))", getElementTextByXPath("//td[@class='colHeaderLink']"));
    }

    public void testRenderPortletTrusted()
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testRenderPortletTrusted",
                "{jiraportlet:url=" + jiraWebTester.getTestContext().getBaseUrl() + "secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:projectstats&projectid=10000&showclosed=false&sortDirection=asc&sortOrder=natural&statistictype=allFixfor}");

        viewPageById(testPageId);

        String jiraPortletHtmlSource = getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraportlet']//iframe", "src")
                .substring(getConfluenceWebTester().getContextPath().length());

        gotoPage(jiraPortletHtmlSource);

        assertEquals("Total Issues: 2 Statistics: Test Project (Fix For Versions (all))", getElementTextByXPath("//td[@class='colHeaderLink']"));
    }

    /**
     * CONFJIRA-125
     */
    public void testNationalCharactersDisplayedProperly() throws IOException
    {
        restoreJiraData("CONFJIRA-125-test-data.xml");

        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testNationalCharactersDisplayedProperly",
                "{jiraportlet:url=" + jiraWebTester.getTestContext().getBaseUrl() + "secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:assignedtome}");

        viewPageById(testPageId);

        String jiraPortletHtmlSource = getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraportlet']//iframe", "src")
                .substring(getConfluenceWebTester().getContextPath().length());

        /* Since jWebUnit does not allow us to read pages in a specific charset, I'd have to do this. Crap. */
        ConfluenceWebTester confluenceWebTester = getConfluenceWebTester();
        String baseUrl = confluenceWebTester.getBaseUrl();

        URLConnection urlConnection = new URL(baseUrl + jiraPortletHtmlSource + "&os_username=admin&os_password=admin").openConnection();

        InputStream portletHtmlInput = null;

        try
        {

            portletHtmlInput = urlConnection.getInputStream();
            String portletHtml = IOUtils.toString(portletHtmlInput); /* Read using Confluence's encoding */

            assertTrue(portletHtml.indexOf("\u201e\u00c9\u00ea\u201e\u00c7\u221e01") >= 0);
            assertTrue(portletHtml.indexOf("\u00cf\u00c9\u00e0\u00ce\u00b0\u00fa\u00cf\u00f6\u00a5 \u00cd\u220f\u221e\u00ce\u00e4\u2022 01") >= 0);

        }
        finally
        {
            IOUtils.closeQuietly(portletHtmlInput);
        }
    }
}
