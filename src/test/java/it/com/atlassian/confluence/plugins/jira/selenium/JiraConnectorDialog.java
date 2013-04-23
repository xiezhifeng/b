package it.com.atlassian.confluence.plugins.jira.selenium;

import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.browsers.AutoInstallClient;

public class JiraConnectorDialog
{
    protected SeleniumClient client;

    private JiraConnectorDialog(SeleniumClient client)
    {
        this.client = client;
    }

    /**
     * Open the JIRA Connector dialog. This is expected to be called from the context of the Editor.
     *
     * @return an 'opened' JiraConnectorDialog instance.
     */
    public static JiraConnectorDialog openDialog(SeleniumClient client)
    {
        AutoInstallClient.assertThat().elementPresentByTimeout("jiralink", 10000);
        client.click("jiralink");
        return new JiraConnectorDialog(client);
    }

    /**
     *
     * @param query the search query to be typed
     * @return the same JiraConnectorDialog with the search tab active and the search results populated.
     */
    public JiraConnectorDialog performSearch(String query)
    {
        client.click("//li/button[text()='Search']");
        client.type("css=input[name='jiraSearch']", query);
        client.click("css=div.jira-search-form button");
        client.waitForAjaxWithJquery();

        return this;
    }

    /**
     * Perform a query and select an indexed result, starting from 1 (as is the case in XPath).
     *
     * @param query the search query to be typed
     * @param resultIndex 1 based index of the row to be selected from the search results.
     * @return the issue key of the selected result
     */
    public String performSearch(String query, int resultIndex)
    {
        performSearch(query);
        int rowIndex = resultIndex + 1;
        client.click("xpath=//div[@id='my-jira-search']//tr[" + rowIndex + "]"); // +1 to avoid the heading row.
        return client.getText("xpath=//div[@id='my-jira-search']//tr[" + rowIndex + "]/td[@class='issue-key-column']/span");
    }

    /**
     * After a search result calling this will check the 'insert all query results as table' checkbox.
     *
     * @return the same JiraConnectorDialog
     */
    public JiraConnectorDialog checkInsertAllForSearchResult()
    {
        client.check("xpath=//input[@name='as-jql']");
        return this;
    }

    /**
     * Click the insert button on the dialog and dismiss the dialog. The dialog is
     * no longer suitable for use after this call.
     */
    public void clickInsert()
    {
        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button");
    }

    public void checkTotalIssueCount() {
        client.clickElementWithClass("jql-display-opts-open");
        client.check("xpath=//input[@id='opt-total']");
    }
}
