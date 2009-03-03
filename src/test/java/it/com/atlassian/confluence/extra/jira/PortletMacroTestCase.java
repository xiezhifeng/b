package it.com.atlassian.confluence.extra.jira;

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
}
