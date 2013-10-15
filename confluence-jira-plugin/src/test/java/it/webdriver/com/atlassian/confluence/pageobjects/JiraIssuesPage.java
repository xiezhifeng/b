package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

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

    public int getIssueCount()
    {
        return getIssuesCountFromText(issuesCount.getText());
    }

    public int getNumberOfIssuesInTable()
    {
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
        return getIssuesCountFromText(issuesTableRowCount.getText());
    }

    public void clickRefreshedIcon()
    {
        refreshedIcon.click();
    }

    private int getIssuesCountFromText(String text)
    {
        return Integer.parseInt(text.split(" ")[0]);
    }
}
