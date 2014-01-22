package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.webdriver.utils.by.ByJquery;
import org.openqa.selenium.By;

import java.util.List;

public class JiraCreatedMacroDialog extends Dialog
{
    @ElementBy(id = "create-issues-form")
    private PageElement createIssueForm;

    @ElementBy(id = "jiralink")
    private PageElement jiraMacroLink;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu")
    private PageElement menu;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu .selected")
    private PageElement selectedMenu;

    @ElementBy(cssSelector = ".project-select")
    private SelectElement project;

    @ElementBy(cssSelector = ".type-select")
    private SelectElement issuesType;

    @ElementBy(cssSelector = ".issue-summary")
    private PageElement summary;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "div[data-jira-type=reporter] > .select2-container > a", timeoutType = TimeoutType.SLOW_PAGE_LOAD)
    private PageElement reporter;

    @ElementBy(cssSelector = ".create-issue-container .jira-error")
    private PageElement jiraErrorMessages;

    public JiraCreatedMacroDialog()
    {
        super("jira-connector");
    }

    public JiraCreatedMacroDialog open()
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

    public SelectElement getProject()
    {
        return project;
    }

    public SelectElement getIssuesType()
    {
        return issuesType;
    }

    public void selectProject(String projectValue)
    {
        Poller.waitUntilTrue("loading projects", project.timed().isEnabled());
        PageElement projectItem = project.find(ByJquery.$("option[value='" + projectValue + "']"));
        Poller.waitUntilTrue(projectItem.timed().isPresent());
        projectItem.click();
    }

    public void selectIssueType(String issueTypeValue)
    {
        PageElement issueTypeItem = issuesType.find(ByJquery.$("option[value='" + issueTypeValue + "']"));
        Poller.waitUntilTrue(issueTypeItem.timed().isPresent());
        issueTypeItem.click();
    }

    public void setEpicName(String epicName)
    {
        PageElement epic = createIssueForm.find(By.cssSelector("div[data-jira-type='com.pyxis.greenhopper.jira:gh-epic-label'] .text"), TimeoutType.SLOW_PAGE_LOAD);
        Poller.waitUntilTrue("Load epic failed", epic.timed().isVisible());
        epic.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        summary.timed().isEnabled();
        summary.type(summaryText);
    }

    public void setReporter(String reporterText)
    {
        reporter.click();

        PageElement popup = pageElementFinder.find(By.cssSelector(".select2-drop-active"));
        PageElement selectInput = popup.find(By.cssSelector("input"));
        selectInput.type(reporterText);
        
        PageElement selectedItem = popup.find(By.cssSelector(".select2-highlighted"));
        selectedItem.click();
    }

    public void submit()
    {
        clickButton("insert-issue-button", false);
    }

    public EditContentPage insertIssue()
    {
        clickButton("insert-issue-button", true);
        return pageBinder.bind(EditContentPage.class);
    }

    public PageElement getSelectedMenu()
    {
        return selectedMenu;
    }

    public TimedQuery<String> getJiraErrorMessages()
    {
        return jiraErrorMessages.timed().getText();
    }

    public Iterable<PageElement> getFieldErrorMessages()
    {
        return pageElementFinder.findAll(By.cssSelector(".error"));
    }

    public TimedQuery<Boolean> isInsertButtonDisabled()
    {
        return insertButton.timed().hasAttribute("disabled", "true");
    }
}
