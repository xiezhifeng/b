package it.com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.plugin.functest.ConfluenceWebTester;
import org.apache.commons.io.IOUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
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
                .substring(getContextPath().length());

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
                .substring(getContextPath().length());

        gotoPage(jiraPortletHtmlSource);

        assertEquals("Total Issues: 2 Statistics: Test Project (Fix For Versions (all))", getElementTextByXPath("//td[@class='colHeaderLink']"));
    }
}
