package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter;


import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;

public abstract class AbstractJiraIssueFilterDialog extends AbstractJiraIssueMacroDialog
{

    @ElementBy(cssSelector = "#my-jira-search form button[title='Search']")
    protected PageElement searchButton;

    public PageElement getSearchButton()
    {
        PageElement searchButton = find(".jira-search-form button[title='Search']");
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        return searchButton;
    }

    public AbstractJiraIssueFilterDialog clickSearchButton()
    {
        Poller.waitUntilTrue(getSearchButton().timed().isVisible());
        getSearchButton().click();
        Poller.waitUntilTrue(searchButton.timed().isEnabled());
        waitUntilNoSpinner();
        return this;
    }

    public void showDisplayOption()
    {
        String filterQuery = "status=open";
        inputJqlSearch(filterQuery);
        Poller.waitUntilTrue(getJqlSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(getSearchButton().timed().isEnabled());
        getSearchButton().click();

        openDisplayOption();
    }

    public void fillMaxIssues(String maxIssuesVal)
    {
        showDisplayOption();
        getMaxIssuesTxt().clear().type(maxIssuesVal);

        // fire click to focusout the text box
        getDisplayOptionPanel().clickDisplayTable();
    }

    public PageElement getMaxIssuesTxt()
    {
        return find("#jira-maximum-issues");
    }
}
