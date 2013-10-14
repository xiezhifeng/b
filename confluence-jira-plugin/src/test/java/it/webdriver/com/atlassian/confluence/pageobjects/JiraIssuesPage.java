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

    @ElementBy(cssSelector = "#main-content .icon-refresh")
    private PageElement refreshedIcon;

    @ElementBy(cssSelector = "#main-content .static-jira-issues_count > .issue-link")
    private PageElement issuesCount;

    public int getIssueCount()
    {
        String issueCountStr = issuesCount.getText().split(" ")[0];
        return Integer.parseInt(issueCountStr);
    }

    public int getNumberOfIssuesInTable()
    {
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
        return issuesTable.findAll(By.cssSelector(".rowNormal")).size()
                + issuesTable.findAll(By.cssSelector(".rowAlternate")).size();
    }

    public void clickRefreshedIcon()
    {
        refreshedIcon.click();
    }

}
