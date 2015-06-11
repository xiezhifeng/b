package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.helper.RestTestHelper;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.mortbay.util.MultiMap;
import org.mortbay.util.UrlEncoded;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraIssueRemoteLinksWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @After
    public void resetRemoteLinks() throws Exception
    {
        deleteRemoteLinks("TP-1");
    }

    @Test
    public void testDoNotCreateRemoteLinksForIssueTable() throws Exception
    {
        createPageWithJiraIssueMacro("key in (TP-1, TP-2)");
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");
        assertTrue("Page with id '" + Page.TEST.getIdAsString() + "' found in " + remoteLinks, !containsLinkWithPageId(remoteLinks, Page.TEST.getIdAsString()));
    }

    @Test
    public void testCreateRemoteLinksForNewPage() throws Exception
    {
        createPageWithJiraIssueMacro("TP-1");
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");
        assertTrue("Page with id '" + Page.TEST.getIdAsString() + "' not found in " + remoteLinks, containsLinkWithPageId(remoteLinks, Page.TEST.getIdAsString()));
    }

    @Test
    public void testCreateRemoteLinksForUpdatedPage() throws Exception
    {
        ViewPage viewPage = editContentPage.save();
        viewPage.edit();

        createPageWithJiraIssueMacro("TP-1");
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");
        assertTrue("Page with id '" + Page.TEST.getIdAsString() + "' not found in " + remoteLinks, containsLinkWithPageId(remoteLinks, Page.TEST.getIdAsString()));
    }

    private void deleteRemoteLinks(String issueKey) throws IOException, JSONException {
        final JSONArray remoteLinks = getJiraRemoteLinks(issueKey);
        for (int i = 0; i < remoteLinks.length(); ++i)
        {
            final JSONObject link = remoteLinks.getJSONObject(i);
            final Long id = link.getLong("id");
            final String url = JIRA_BASE_URL + "/rest/api/latest/issue/" + issueKey + "/remotelink/" + id;
            CloseableHttpResponse response = RestTestHelper.deleteRestResponse(RestTestHelper.getDefaultUser(), url);

            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Got status " + status + " when retrieving " + url, 204, status);
        }
    }

    private JSONArray getJiraRemoteLinks(String issueKey) throws IOException
    {
        final String url = JIRA_BASE_URL + "/rest/api/latest/issue/" + issueKey + "/remotelink" + ApplinkHelper.getAuthQueryString();
        GetMethod m = new GetMethod(url);
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        int status = client.executeMethod(m);
        Assert.assertEquals("Got status " + status + " when retrieving " + url, 200, status);

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
