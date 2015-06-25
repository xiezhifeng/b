package com.atlassian.confluence.plugins.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.apache.commons.lang3.StringUtils.split;

public class JiraIssuesPage extends ViewPage
{
    @ElementBy(cssSelector = "#main-content table.aui")
    private PageElement issuesTable;

    @ElementBy(cssSelector = "#main-content table.aui .jira-tablesorter-header")
    private PageElement headerIssueTable;

    @ElementBy(cssSelector = "#main-content .icon-refresh")
    private PageElement refreshedIcon;

    @ElementBy(cssSelector = "#main-content .static-jira-issues_count > .issue-link")
    private PageElement issuesCount;

    @ElementBy(cssSelector = ".refresh-issues-bottom [id^=total-issues-count] a")
    private PageElement issuesTableRowCount;

    @ElementBy(id = "main-content")
    private PageElement main;

    @ElementBy(cssSelector = ".jim-sortable-dark-layout")
    private PageElement sortableDarkLayout;

    @ElementBy(cssSelector = ".jira-issue")
    private PageElement singleJiraIssue;

    @ElementBy(cssSelector = ".jiraissues_table .flexigrid")
    private PageElement dynamicJiraIssueTable;

    @ElementBy(cssSelector = ".jim-error-message a")
    private PageElement jiraErrorLink;

    @ElementBy(cssSelector = ".jim-error-message")
    private PageElement jiraErrorMessage;

    public int getIssueCount()
    {
        return getIssuesCountFromText(issuesCount.getText());
    }

    public int getNumberOfIssuesInTable()
    {
        return getIssuesCountFromText(getNumberOfIssuesText());
    }

    public String getNumberOfIssuesText()
    {
        waitUntilFalse(sortableDarkLayout.timed().isVisible());
        return issuesTableRowCount.getText();
    }

    public void clickRefreshedIcon()
    {
        refreshedIcon.click();
    }

    private int getIssuesCountFromText(String text)
    {
        return Integer.parseInt(split(text, " ")[0]);
    }
    
    public void clickColumnHeaderIssueTable(String columnName)
    {
        
        List<PageElement> columns = getIssuesTableColumns();
        for (PageElement column : columns)
        {
            if (column.find(By.cssSelector(".jim-table-header-content")).getText().trim().equalsIgnoreCase(columnName))
            {
                column.click();
                break;
            }
        }
    }

    public PageElement getDynamicJiraIssueTable() {
        return dynamicJiraIssueTable;
    }

    public List<PageElement> getIssuesTableColumns()
    {
        return issuesTable.findAll(By.cssSelector(".jira-tablesorter-header"));
    }

    public String getFirstRowValueOfSummay() 
    {
        waitUntilTrue("JIRA issues table is not visible", issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[2]/a")).getText();
    }

    public boolean isSingleContainText(String text)
    {
        waitUntilTrue("Single JIRA issue is not visible", singleJiraIssue.timed().isVisible());
        return singleJiraIssue.getText().contains(text);
    }

    public PageElement getIssuesTableElement()
    {
        return issuesTable;
    }

    public PageElement getIssuesCountElement()
    {
        return issuesCount;
    }

    public PageElement getRefreshedIconElement()
    {
        return refreshedIcon;
    }

    public PageElement getJiraErrorLink()
    {
        waitUntilTrue(jiraErrorLink.timed().isVisible());
        return jiraErrorLink;
    }

    public PageElement getErrorMessage()
    {
        waitUntilTrue(jiraErrorMessage.timed().isVisible());
        return jiraErrorMessage;
    }

    public String getFirstRowValueOfAssignee()
    {
        waitUntilTrue(issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[7]")).getText();
    }
}
