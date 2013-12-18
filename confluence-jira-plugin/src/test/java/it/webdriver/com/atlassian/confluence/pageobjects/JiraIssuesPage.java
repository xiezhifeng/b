package it.webdriver.com.atlassian.confluence.pageobjects;

import org.openqa.selenium.By;

import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesPage extends ViewPage
{

    @ElementBy(cssSelector = "#main-content table.aui")
    private PageElement issuesTable;

    @ElementBy(cssSelector = "#main-content table.aui tablesorter-header")
    private PageElement headerIssueTable;

    @ElementBy(cssSelector = "#main-content .icon-refresh")
    private PageElement refreshedIcon;

    @ElementBy(cssSelector = "#main-content .static-jira-issues_count > .issue-link")
    private PageElement issuesCount;

    @ElementBy(cssSelector = ".refresh-issues-bottom [id^=total-issues-count] a")
    private PageElement issuesTableRowCount;

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
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
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
    
    public void clickHeaderIssueTable(String header)
    {
        if (headerIssueTable.find(By.cssSelector(".jim-table-header-content")).getText().equalsIgnoreCase(header))
        {
            headerIssueTable.click();
        }
    }

    public boolean isSorted(String header)
    {
        if (headerIssueTable.hasClass("tablesorter-headerDesc"))
        {
            return headerIssueTable.find(By.cssSelector(".jim-table-header-content")).getText().equalsIgnoreCase(header);
        }
        return false;
    }
}
