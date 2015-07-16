package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

import com.atlassian.pageobjects.elements.query.Poller;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.apache.commons.lang3.StringUtils.split;

public class JiraIssuesPage extends ViewPage
{
    @ElementBy(cssSelector = "#main-content table.aui")
    private PageElement issuesTable;

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


    /**
     * Click on column table and wait to finish
     * @param columnName clicked column
     * @param expectedSortType accept two values: Asc\Desc, null to avoid waiting to complete
     */
    public void clickColumnHeaderIssueTable(String columnName, String expectedSortType)
    {
        PageElement columnElement = getColumn(columnName);
        columnElement.click();
        if (StringUtils.isNotEmpty(expectedSortType))
        {
            Poller.waitUntilTrue(columnElement.timed().hasClass("tablesorter-header" + expectedSortType));
        }
    }

    public PageElement getColumn(String columnName)
    {
        List<PageElement> columns = getIssuesTableColumns();
        for (PageElement column : columns)
        {
            if (StringUtils.equalsIgnoreCase(column.getText().trim(), columnName))
            {
                return column;
            }
        }
        return null;
    }

    public PageElement getDynamicJiraIssueTable()
    {
        return dynamicJiraIssueTable;
    }

    public List<PageElement> getIssuesTableColumns()
    {
        return issuesTable.findAll(By.cssSelector("th.jira-macro-table-underline-pdfexport"));
    }

    public String getFirstRowValueOfSummay() 
    {
        return getValueInTable(2, 1);
    }

    public boolean isSingleContainText(String text)
    {
        Poller.waitUntilTrue("Single JIRA issue is not visible", singleJiraIssue.timed().isVisible());
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
        return getValueInTable(7, 1);
    }

    public String getValueInTable(int column, int row)
    {
        int tableRowOffset = 2; //one row for header, and one row is kept by confluence above header with empty data
        waitUntilTrue("JIRA issues table is not visible", issuesTable.timed().isPresent());
        String xpathSelector = String.format("//table[@class='aui']/tbody/tr[%s]/td[%s]", row + tableRowOffset, column);
        return main.find(By.xpath(xpathSelector)).getText();
    }
}
