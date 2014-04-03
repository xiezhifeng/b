package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.hamcrest.Matchers;
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
    private SelectElement projectSelect;

    @ElementBy(cssSelector = ".project-select option[value='10011']")
    private SelectElement testProjectOption;

    @ElementBy(cssSelector = ".issuetype-select")
    private SelectElement issuesTypeSelect;

    @ElementBy(name = "summary")
    private PageElement summary;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "div[data-jira-type=components] > .select2-container", timeoutType = TimeoutType.SLOW_PAGE_LOAD)
    private PageElement components;
    
    @ElementBy(cssSelector = ".create-issue-container .warning")
    private PageElement jiraErrorMessages;

    @ElementBy(id = "select2-drop")
    private PageElement select2Dropdown;

    @ElementBy(name = "customfield_10017")
    private PageElement epicField;

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

    public void selectProject(String projectName)
    {
        Picker projectPicker = getPicker(projectSelect);
        projectPicker.openDropdown();

        projectPicker.chooseOption(projectName);
        Poller.waitUntil(projectPicker.getSelectedOption().timed().getText(), Matchers.containsString(projectName),
                Poller.by(20000));

    }

    public List<String> getAllProjects()
    {
        Picker projectPicker = getPicker(projectSelect);
        projectPicker.openDropdown();
        return projectPicker.getAllOptions();
    }

    public Picker getPicker(PageElement selecteElement)
    {
        Picker picker = pageBinder.bind(Picker.class);
        picker.bindingElements(selecteElement);
        return picker;
    }

    public void selectIssueType(String issueTypeName)
    {
        Picker issueTypePicker = getPicker(issuesTypeSelect);
        issueTypePicker.openDropdown();

        issueTypePicker.chooseOption(issueTypeName);
        Poller.waitUntil(issueTypePicker.getSelectedOption().timed().getText(), Matchers.containsString(issueTypeName),
                Poller.by(20000));

    }

    public List<String> getAllIssueTypes()
    {
        Picker issueTypePicker = getPicker(issuesTypeSelect);
        issueTypePicker.openDropdown();
        return issueTypePicker.getAllOptions();
    }

    public void setEpicName(String epicName)
    {
        Poller.waitUntilTrue("Load epic failed", epicField.timed().isVisible());
        epicField.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        Poller.waitUntilTrue(summary.timed().isEnabled());
        summary.type(summaryText);
    }

    public void setDuedate(String duedate)
    {
        Poller.waitUntilTrue(pageElementFinder.find(By.cssSelector("div[data-jira-type=duedate] input")).timed().isVisible());
        PageElement datepicker = pageElementFinder.find(By.cssSelector("div[data-jira-type=duedate] input"));
        datepicker.type(duedate);
    }

    public void submit()
    {
        insertButton.click();
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

    public TimedQuery<Boolean> isInsertButtonEnabled()
    {
        return insertButton.timed().isEnabled();
    }

    public PageElement getComponents()
    {
        return components;
    }

    public void waitUntilProjectLoaded()
    {
        // Wait for the option which has value is 10011 loaded.
        Poller.waitUntilTrue(testProjectOption.timed().isVisible());
    }
}
