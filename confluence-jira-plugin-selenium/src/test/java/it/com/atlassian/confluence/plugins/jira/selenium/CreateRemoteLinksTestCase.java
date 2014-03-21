package it.com.atlassian.confluence.plugins.jira.selenium;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.jetty.util.MultiMap;
import org.openqa.jetty.util.UrlEncoded;

import java.io.IOException;
import java.util.List;

/**
 * This class contains tests for JIRA remote issue linking.
 */
public class CreateRemoteLinksTestCase extends AbstractJiraPanelTestCase
{
    private final HttpClient httpClient = new HttpClient();

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
        deleteRemoteLinks("TP-1");
    }

    public void testDoNotCreateRemoteLinksForIssueTable() throws Exception
    {
        addIssueTable("key in (TP-1, TP-2)");

        final String pageId = createPage();
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");

        assertTrue("Page with id '" + pageId + "' found in " + remoteLinks, !containsLinkWithPageId(remoteLinks, pageId));
    }

    public void testCreateRemoteLinksForNewPage() throws Exception
    {
        addIssueLink("TP-1");

        final String pageId = createPage();
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");

        assertTrue("Page with id '" + pageId + "' not found in " + remoteLinks, containsLinkWithPageId(remoteLinks, pageId));

    }

    public void testCreateRemoteLinksForUpdatedPage() throws Exception
    {
        final String pageId = createPage();
        // Click on the linkID and no need to wait for page to load because of quick-edit
        client.click("css=#editPageLink");
        client.waitForCondition("window.AJS && window.AJS.Editor", 10000);

        addIssueLink("TP-1");
        client.click("css=#rte-button-publish");
        client.waitForPageToLoad();

        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");

        assertTrue("Page with id '" + pageId + "' not found in " + remoteLinks, containsLinkWithPageId(remoteLinks, pageId));
    }

    private String createPage()
    {
        // Set title
        client.click("css=#content-title");
        final String contentId = client.getEval("window.AJS.Confluence.Editor.getContentId()");
        client.typeKeys("css=#content-title", "Test " + contentId);

        // Save page in default location
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-publish");
       // client.click("css=button.move-button", true);
        client.waitForPageToLoad();
        return client.getEval("window.AJS.Meta.get('page-id')");
    }

    private void addIssueLink(String key)
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", key);

        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        assertEquals(key, client.getTable("css=#my-jira-search table.my-result.1.1"));

        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
    }

    private void addIssueTable(String query, String... expected)
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", query);

        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        for (int i = 0; i < expected.length; ++i)
        {
            assertEquals(expected[i], client.getTable("css=#my-jira-search table.my-result." + (i + 1) + ".1"));
        }

//        client.check("as-jql", "as-jql");
    	client.check("insert-advanced", "insert-table");

        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button");
    }

    private JSONArray getJiraRemoteLinks(String issueKey) throws IOException
    {
        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);

        HttpClient client = new HttpClient();
        String baseUrl = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

        final String url = baseUrl + "/rest/api/latest/issue/" + issueKey + "/remotelink" + authArgs;
        GetMethod m = new GetMethod(url);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");

        int status = client.executeMethod(m);
        assertEquals("Got status " + status + " when retrieving " + url, 200, status);

        final String responseBody = m.getResponseBodyAsString();

        m.releaseConnection();

        try
        {
            return new JSONArray(responseBody);
        }
        catch (JSONException e)
        {
            throw new RuntimeException("Invalid JSON: " + responseBody, e);
        }
    }



    private void deleteRemoteLinks(String issueKey) throws IOException
    {
        final JSONArray remoteLinks = getJiraRemoteLinks(issueKey);

        try
        {
            for (int i = 0; i < remoteLinks.length(); ++i)
            {
                final JSONObject link = remoteLinks.getJSONObject(i);
                final Long id = link.getLong("id");

                String adminUserName = getConfluenceWebTester().getAdminUserName();
                String adminPassword = getConfluenceWebTester().getAdminPassword();
                String authArgs = getAuthQueryString(adminUserName, adminPassword);


                String baseUrl = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

                final String url = baseUrl + "/rest/api/latest/issue/" + issueKey + "/remotelink/" + id + authArgs;
                DeleteMethod m = new DeleteMethod(url);

                int status = httpClient.executeMethod(m);
                assertEquals("Got status " + status + " when retrieving " + url, 204, status);

                m.releaseConnection();
            }
        } catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean containsLinkWithPageId(JSONArray remoteLinks, String pageId)
    {
        for (int i = 0; i < remoteLinks.length(); ++i)
        {
            try
            {
                final JSONObject link = remoteLinks.getJSONObject(i);

                final String globalId = link.getString("globalId");
                final MultiMap components = new UrlEncoded(globalId, "UTF-8");
                final List<?> values = components.getValues("pageId");
                if (values.contains(pageId))
                {
                    return true;
                }
            }
            catch (JSONException e)
            {
                throw new RuntimeException("Invalid JSON: " + remoteLinks, e);
            }
        }
        return false;
    }
}
