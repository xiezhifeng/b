package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.List;

import org.openqa.selenium.By;

import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

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
        
        List<PageElement> headers = issuesTable.findAll(By.cssSelector(".jira-tablesorter-header"));
        for (PageElement column : headers)
        {
            if (column.find(By.cssSelector(".jim-table-header-content")).getText().trim().equalsIgnoreCase(header))
            {
                column.click();
                break;
            }
        }
    }

    public String getFirstRowValueOfSummay() 
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[2]/a")).getText();
        
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
}
