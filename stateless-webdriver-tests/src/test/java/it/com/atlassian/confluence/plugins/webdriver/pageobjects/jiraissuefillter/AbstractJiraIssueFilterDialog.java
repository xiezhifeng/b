package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter;


import com.atlassian.pageobjects.elements.query.TimedCondition;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;

import org.openqa.selenium.By;

public abstract class AbstractJiraIssueFilterDialog extends AbstractJiraIssueMacroDialog
{
    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    protected PageElement insertButton;

    @ElementBy(cssSelector = "#my-jira-search form button[title='Search']")
    protected PageElement searchButton;

    public AbstractJiraIssueFilterDialog()
    {
        super("jira-connector");
    }

    public void submit()
    {
        insertButton.click();
    }

    public TimedQuery<Boolean> isInsertButtonEnabled()
    {
        return insertButton.timed().isEnabled();
}

    public PageElement getInsertButton()
    {
        return insertButton;
    }
    
    public PageElement getJiraChartMacroAnchor()
    {
        return find("#open-jira-chart-dialog");
    }

    public PieChartDialog clickJiraChartMacroAnchor()
    {
        getJiraChartMacroAnchor().click();
        PieChartDialog pieChartDialog = this.pageBinder.bind(PieChartDialog.class);
        Poller.waitUntilTrue(pieChartDialog.isVisibleTimed());
        return pieChartDialog;
    }

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

    public TimedCondition hasInsertButton(){
        return insertButton.timed().isVisible();
    }

    public boolean isInsertable()
    {
        return insertButton.isEnabled();
    }
}
