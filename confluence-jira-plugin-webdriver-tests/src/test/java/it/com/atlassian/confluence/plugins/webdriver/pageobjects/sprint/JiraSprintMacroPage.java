package it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint;

import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

public class JiraSprintMacroPage extends ViewPage
{
    @ElementBy(cssSelector = ".jira-issue.jira-sprint")
    private PageElement sprint;

    public String getSprintName()
    {
        return sprint.find(By.className("jira-issue-key")).getText().trim();
    }

    public String getSprintStatus()
    {
        return sprint.find(By.className("aui-lozenge")).getText().trim();
    }

    public String getSprintLink()
    {
        return sprint.find(By.className("jira-issue-key")).getAttribute("href");
    }
}
