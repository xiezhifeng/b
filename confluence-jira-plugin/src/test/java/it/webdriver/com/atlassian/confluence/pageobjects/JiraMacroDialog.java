package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;
import org.openqa.selenium.By;

import java.util.List;

public class JiraMacroDialog extends Dialog
{
    @ElementBy(id = "create-issues-form")
    private PageElement createIssueForm;

    @ElementBy(id = "jiralink")
    private PageElement jiraMacroLink;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu")
    private PageElement menu;

    @ElementBy(cssSelector = ".project-select")
    private SelectElement project;

    @ElementBy(cssSelector = ".type-select")
    private SelectElement issuesType;

    @ElementBy(cssSelector = ".issue-summary")
    private PageElement summary;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    public JiraMacroDialog()
    {
        super("jira-connector");
    }
    
    public JiraMacroDialog open()
    {
        jiraMacroLink.click();
        return this;
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

    public void selectProject(String projectName)
    {
        Poller.waitUntilTrue("loading projects", project.timed().isEnabled());
        project.select(Options.text(projectName));
    }

    public void selectIssueType(String issueTypeName)
    {
        issuesType.select(Options.text(issueTypeName));
    }

    public void setEpicName(String epicName)
    {
        PageElement epic = createIssueForm.find(ByJquery.$("div[data-jira-type='com.pyxis.greenhopper.jira:gh-epic-label'] .text"));
        epic.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        summary.type(summaryText);
    }

    public EditContentPage insertIssue()
    {
        clickButton("insert-issue-button", true);
        return pageBinder.bind(EditContentPage.class);
    }

}
