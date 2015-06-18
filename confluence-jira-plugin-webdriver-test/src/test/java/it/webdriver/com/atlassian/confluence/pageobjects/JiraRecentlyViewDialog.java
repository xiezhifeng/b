package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import java.util.List;

public class JiraRecentlyViewDialog extends Dialog
{

    @ElementBy(id = "jiralink")
    private PageElement jiraMacroLink;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu")
    private PageElement menu;

    @ElementBy(cssSelector = ".jiraSearchResults")
    private PageElement issuesTable;

    public JiraRecentlyViewDialog()
    {
        super("jira-connector");
    }

    public JiraRecentlyViewDialog open()
    {
        jiraMacroLink.click();
        return this;
    }

    public boolean isResultContainIssueKey(String issueKey)
    {
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
        return issuesTable.getText().contains(issueKey);
    }

    public void selectMenuItem(String menuItemText)
    {
        List<PageElement> menuItems = menu.findAll(By.tagName("button"));
        for(PageElement menuItem : menuItems)
        {
            if(menuItemText.equals(menuItem.getText()))
            {
                menuItem.click();
                break;
            }
        }
    }

}
