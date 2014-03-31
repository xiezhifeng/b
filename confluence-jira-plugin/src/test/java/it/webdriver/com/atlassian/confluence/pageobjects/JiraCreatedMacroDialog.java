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

import java.util.ArrayList;
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

    @ElementBy(cssSelector = ".project-select option[value='10011']")
    private SelectElement testProjectOption;

    @ElementBy(cssSelector = ".issuetype-select")
    private SelectElement issuesType;

    @ElementBy(name = "summary")
    private PageElement summary;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "div[data-jira-type=reporter] > .select2-container > a", timeoutType = TimeoutType.SLOW_PAGE_LOAD)
    private PageElement reporter;

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
        Poller.waitUntilTrue("Load epic failed", epicField.timed().isVisible());
        epicField.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        Poller.waitUntilTrue(summary.timed().isVisible());
        summary.type(summaryText);
    }

    public void setReporter(String reporter)
    {
        searchReporter(reporter);
        chooseReporter(reporter);
    }

    public void searchReporter(String reporterValue)
    {
        Poller.waitUntilTrue(reporter.timed().isVisible());
        reporter.click();
        Poller.waitUntilTrue(select2Dropdown.timed().isVisible());
        PageElement searchInput = select2Dropdown.find(By.cssSelector("input"));
        searchInput.type(reporterValue);
        // wait for result list was displayed with highlighted option
        Poller.waitUntilTrue(select2Dropdown.find(By.cssSelector(".select2-highlighted")).timed().isVisible());
    }

    public List<String> getReporterList()
    {
        List<String> reporters = new ArrayList<String>();
        List<PageElement> options = select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            reporters.add(option.getText());
        }
        return reporters;
    }

    public void chooseReporter(String reporterValue)
    {
        List<PageElement> options = select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if (option.getText().contains(reporterValue))
            {
                option.click();
                break;
            }
        }
    }

    public String getReporterText()
    {
        return reporter.getText();
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

    public TimedQuery<Boolean> isInsertButtonDisabled()
    {
        return insertButton.timed().hasAttribute("disabled", "true");
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
