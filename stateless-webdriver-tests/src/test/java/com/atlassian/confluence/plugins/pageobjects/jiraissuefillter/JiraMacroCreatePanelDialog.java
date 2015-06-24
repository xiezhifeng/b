package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;

import com.atlassian.confluence.plugins.pageobjects.Select2Element;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.TimedQuery;

import org.openqa.selenium.By;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.atlassian.pageobjects.elements.timeout.TimeoutType.SLOW_PAGE_LOAD;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class JiraMacroCreatePanelDialog extends JiraIssueFilterDialog
{
    @ElementBy(className = "create-issue-container")
    protected PageElement createIssueContainer;

    @ElementBy(cssSelector = ".project-select")
    protected SelectElement projectSelect;

    @ElementBy(cssSelector = ".issuetype-select")
    protected SelectElement issuesTypeSelect;

    @ElementBy(name = "summary")
    private PageElement summary;

    @ElementBy(cssSelector = "div[data-jira-type=components] > .select2-container", timeoutType = SLOW_PAGE_LOAD)
    protected PageElement components;

    @ElementBy(cssSelector = ".create-issue-container .warning")
    protected PageElement jiraErrorMessages;

    @ElementBy(id = "select2-drop")
    protected PageElement select2Dropdown;

    @ElementBy(cssSelector = "div[data-jira-type=\"com.pyxis.greenhopper.jira:gh-epic-label\"] > input[type=text]")
    protected PageElement epicField;

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

    public Select2Element getSelect2Element(PageElement selectElement)
    {
        return pageBinder.bind(Select2Element.class, selectElement);
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

    public void clearSummary()
    {
        waitUntilTrue("Summary field is not enabled", summary.timed().isEnabled());
        summary.clear();
    }

    public void setDuedate(String duedate)
    {
        waitUntilTrue("Due date field is not visible", pageElementFinder.find(By.cssSelector("div[data-jira-type=duedate] input")).timed().isVisible());
        PageElement datepicker = pageElementFinder.find(By.cssSelector("div[data-jira-type=duedate] input"));
        datepicker.type(duedate);
    }

    public EditContentPage insertIssue()
    {
        clickButton("insert-issue-button", true);
        return pageBinder.bind(EditContentPage.class);
    }

    public TimedQuery<String> getJiraErrorMessages()
    {
        return jiraErrorMessages.timed().getText();
    }

    public Iterable<PageElement> getFieldErrorMessages()
    {
        return pageElementFinder.findAll(By.cssSelector(".error"));
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