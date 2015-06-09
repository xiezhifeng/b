package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.List;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.TimedQuery;

import org.openqa.selenium.By;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.atlassian.pageobjects.elements.timeout.TimeoutType.SLOW_PAGE_LOAD;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class JiraCreatedMacroDialog extends Dialog
{
    @ElementBy(className = "create-issue-container")
    private PageElement createIssueContainer;

    @ElementBy(id = "jiralink")
    private PageElement jiraMacroLink;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu")
    private PageElement menu;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu .selected")
    private PageElement selectedMenu;

    @ElementBy(cssSelector = ".project-select")
    private SelectElement projectSelect;

    @ElementBy(cssSelector = ".issuetype-select")
    private SelectElement issuesTypeSelect;

    @ElementBy(name = "summary")
    private PageElement summary;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "div[data-jira-type=components] > .select2-container", timeoutType = SLOW_PAGE_LOAD)
    private PageElement components;
    
    @ElementBy(cssSelector = ".create-issue-container .warning")
    private PageElement jiraErrorMessages;

    @ElementBy(id = "select2-drop")
    private PageElement select2Dropdown;

    @ElementBy(cssSelector = "div[data-jira-type=\"com.pyxis.greenhopper.jira:gh-epic-label\"] > input[type=text]")
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
        Select2Element projectSelector = getSelect2Element(projectSelect);
        projectSelector.openDropdown();

        projectSelector.chooseOption(projectName);
        waitUntil(projectSelector.getSelectedOption().timed().getText(), containsString(projectName),
                by(20000));
    }

    public List<String> getAllProjects()
    {
        Select2Element projectSelector = getSelect2Element(projectSelect);
        projectSelector.openDropdown();
        List<String> projects =  projectSelector.getAllOptions();
        projectSelector.closeDropdown();
        return projects;
    }

    public Select2Element getSelect2Element(PageElement selecteElement)
    {
        Select2Element select2Element = pageBinder.bind(Select2Element.class);
        select2Element.bindingElements(selecteElement);
        return select2Element;
    }

    public void selectIssueType(String issueTypeName)
    {
        Select2Element issueTypeDropdown = getSelect2Element(issuesTypeSelect);
        issueTypeDropdown.openDropdown();

        issueTypeDropdown.chooseOption(issueTypeName);
        waitUntil("Issue type field doesn't contain " + issueTypeName, issueTypeDropdown.getSelectedOption().timed().getText(), containsString(issueTypeName), by(20000));
    }

    public List<String> getAllIssueTypes()
    {
        Select2Element issueTypeSelect2 = getSelect2Element(issuesTypeSelect);
        issueTypeSelect2.openDropdown();
        List<String> issueTypes = issueTypeSelect2.getAllOptions();
        issueTypeSelect2.closeDropdown();
        return issueTypes;
    }

    public void setEpicName(String epicName)
    {
        waitUntilTrue("Epic field is not visible", epicField.timed().isVisible());
        epicField.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        waitUntilTrue("Summary field is not enabled", summary.timed().isEnabled());
        summary.type(summaryText);
    }

    public void setDuedate(String duedate)
    {
        waitUntilTrue("Due date field is not visible", pageElementFinder.find(By.cssSelector("div[data-jira-type=duedate] input")).timed().isVisible());
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

    public void waitUntilProjectLoaded(String projectId)
    {
        PageElement projectOption = createIssueContainer.find(By.cssSelector(".project-select option[value='" + projectId + "']"));
        waitUntil("Project selection field is not visible", projectOption.timed().isVisible(), is(true), by(15, SECONDS));
    }
}
