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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;

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
        Select2Element projectSelect2 = getSelect2Element(By.cssSelector(".project-select"));
        projectSelect2.openDropdown();

        projectSelect2.chooseOption(projectName);
        Poller.waitUntil(projectSelect2.getSelectedOption().timed().getText(), Matchers.containsString(projectName),
                Poller.by(20000));
    }

    public List<String> getAllProjects()
    {
        Select2Element projectSelect2 = getSelect2Element(By.cssSelector(".project-select"));
        projectSelect2.openDropdown();
        List<String> projects =  projectSelect2.getAllOptions();
        projectSelect2.closeDropdown();
        return projects;
    }

    public Select2Element getSelect2Element(By byElement)
    {
        PageElement select2Ele = getDialog().find(byElement);
        Poller.waitUntilTrue(select2Ele.timed().isVisible());
        Select2Element select2Element = pageBinder.bind(Select2Element.class);
        select2Element.bindingElements(select2Ele);
        return select2Element;
    }

    public void selectIssueType(String issueTypeName)
    {
        Select2Element issueTypeSelect2 = getSelect2Element(By.cssSelector(".issuetype-select"));
        issueTypeSelect2.openDropdown();

        issueTypeSelect2.chooseOption(issueTypeName);
        Poller.waitUntil(issueTypeSelect2.getSelectedOption().timed().getText(), Matchers.containsString(issueTypeName),
                Poller.by(20000));

    }

    public List<String> getAllIssueTypes()
    {
        Select2Element issueTypeSelect2 = getSelect2Element(By.cssSelector(".issuetype-select"));
        issueTypeSelect2.openDropdown();
        List<String> issueTypes = issueTypeSelect2.getAllOptions();
        issueTypeSelect2.closeDropdown();
        return issueTypes;
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

    public void waitUntilProjectLoaded(String projectId)
    {
        PageElement projectOption = getDialog().find(By.cssSelector(".project-select option[value='" + projectId + "']"));
        Poller.waitUntil(projectOption.timed().isVisible(), is(true), Poller.by(15, TimeUnit.SECONDS));
    }
}
