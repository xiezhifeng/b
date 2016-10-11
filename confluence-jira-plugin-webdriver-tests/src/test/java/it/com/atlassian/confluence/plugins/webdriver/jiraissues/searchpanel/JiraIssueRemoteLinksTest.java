package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.util.TimeUtils;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.Editor;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.google.common.base.Supplier;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mortbay.util.MultiMap;
import org.mortbay.util.UrlEncoded;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class JiraIssueRemoteLinksTest extends AbstractJiraIssuesSearchPanelTest
{
    @After
    public void resetRemoteLinks() throws Exception
    {
        deleteRemoteLinks("TP-1");
    }

    @Test
    public void testDoNotCreateRemoteLinksForIssueTable() throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro("key in (TP-1, TP-2)");
        waitUntilFalse("Page with id '" + viewPage.getPageId() + "' found in remote links.", remoteLinksCondition(viewPage));
    }

    @Test
    public void testCreateRemoteLinksForNewPage() throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro("TP-1");
        waitUntilTrue("Page with id '" + viewPage.getPageId() + "' not found in remote links.", remoteLinksCondition(viewPage));
    }

    @Test
    public void testCreateRemoteLinksForUpdatedPage() throws Exception
    {
        ViewPage viewPage = editPage.save();
        viewPage.edit();

        createPageWithJiraIssueMacro("TP-1");
        waitUntilTrue("Page with id '" + viewPage.getPageId() + "' not found in remote links.", remoteLinksCondition(viewPage));
    }

    @Test
    public void testRemoteLinksAreDeletedWhenMacroIsRemoved() throws Exception
    {
        // Reset the page from any previous tests
        editPage.getEditor().clickSaveAndWaitForPageChange();
        gotoEditTestPage(user.get());

        final ViewPage viewPage = createPageWithJiraIssueMacro("TP-1");
        final JSONArray remoteLinks = getJiraRemoteLinks("TP-1");

        // Check link was created
        waitUntilTrue("Page with id '" + viewPage.getPageId() + "' not found in remote links.", remoteLinksCondition(viewPage));

        Editor editorPage = viewPage.edit().getEditor();
        editorPage.getContent().setContent("");
        editorPage.clickSaveAndWaitForPageChange();

        // Check link was deleted
        final JSONArray updatedRemoteLinks = getJiraRemoteLinks("TP-1");
        waitUntilFalse("Page with id '" + viewPage.getPageId() + "' should not be found in remote links.", remoteLinksCondition(viewPage));
    }

    @Test
    public void testRemoteLinksAreNotDeletedWhenOnlyOneMacroIsRemoved() throws Exception
    {
        // Reset the page from any previous tests
        editPage.getEditor().clickSaveAndWaitForPageChange();
        gotoEditTestPage(user.get());

        // Add two macros to the page
        addJiraIssueMacroToPage("TP-1", false);
        EditContentPage editContentPage = addJiraIssueMacroToPage("TP-1", false);

        editContentPage.save();
        JiraIssuesPage viewPage = bindCurrentPageToJiraIssues();

        // Check link was created
        waitUntilTrue("Page with id '" + viewPage.getPageId() + "' not found in remote links.", remoteLinksCondition(viewPage));

        // Delete one of the macros
        Editor editorPage = viewPage.edit().getEditor();
        editorPage.getContent().selectFirstElementWithSelector("img[data-macro-name=\"jira\"]");
        editorPage.getContent().replaceCurrentSelectionText("");
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        // Check link was deleted
        waitUntilFalse("Page with id '" + viewPage.getPageId() + "' should not be found in remote links.", remoteLinksCondition(viewPage));
    }

    private TimedQuery<Boolean> remoteLinksCondition(ViewPage viewPage){
        return Conditions.forSupplier(5000L,
                () -> {
                    final JSONArray remoteLinks;
                    try {
                        remoteLinks = getJiraRemoteLinks("TP-1");
                        return containsLinkWithPageId(remoteLinks, String.valueOf(viewPage.getPageId()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
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
                final String url = JIRA_BASE_URL + "/rest/api/latest/issue/" + issueKey + "/remotelink/" + id + getAuthQueryString();
                DeleteMethod m = new DeleteMethod(url);

                int status = client.executeMethod(m);
                Assert.assertEquals("Got status " + status + " when retrieving " + url, 204, status);
                m.releaseConnection();
            }
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    private JSONArray getJiraRemoteLinks(String issueKey) throws IOException
    {
        final String url = JIRA_BASE_URL + "/rest/api/latest/issue/" + issueKey + "/remotelink" + getAuthQueryString();
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
