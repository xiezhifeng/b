package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import java.util.List;

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

    @ElementBy(cssSelector = ".aui-message.warning.jim-inline-block a")
    private PageElement jiraErrorLink;

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
        Poller.waitUntilFalse(sortableDarkLayout.timed().isVisible());
        return issuesTableRowCount.getText();
    }

    public void clickRefreshedIcon()
    {
        refreshedIcon.click();
    }

    private int getIssuesCountFromText(String text)
    {
        return Integer.parseInt(text.split(" ")[0]);
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
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[2]/a")).getText();
        
    }

    public boolean isSingleContainText(String text)
    {
        Poller.waitUntilTrue(singleJiraIssue.timed().isVisible());
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
        Poller.waitUntilTrue(jiraErrorLink.timed().isVisible());
        return jiraErrorLink;
    }
}
