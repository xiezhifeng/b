package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
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

    @ElementBy(cssSelector = "#jira-connector .dialog-title")
    private PageElement dialogTitle;

    @ElementBy(cssSelector = "button[title='Search']")
    private PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    private PageElement jqlSearch;

    @ElementBy(className = ".jiraSearchResults")
    private PageElement issuesTable;

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
        PageElement epic = createIssueForm.find(ByJquery.$("div[data-jira-type='com.pyxis.greenhopper.jira:gh-epic-label'] .text"));
        Poller.waitUntilTrue("load epic form", epic.timed().isVisible());
        epic.type(epicName);
    }

    public void setSummary(String summaryText)
    {
        summary.timed().isEnabled();
        summary.type(summaryText);
    }

    public String getTitleDialog()
    {
        return dialogTitle.getText();
    }

    public JiraMacroDialog inputJqlSearch(String val)
    {
        Poller.waitUntilTrue(jqlSearch.timed().isVisible());
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }

    public JiraMacroDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }

    public String getJqlSearch()
    {
        return jqlSearch.getValue();
    }

    public PageElement getJQLSearchElement()
    {
        return jqlSearch;
    }

    public PageElement getSearchButton()
    {
        return searchButton;
    }

    public PageElement getIssuesTable()
    {
        return issuesTable;
    }

    public EditContentPage clickInsertDialog()
    {
        Poller.waitUntilTrue(insertButton.timed().isEnabled());
        clickButton("insert-issue-button", true);
        return pageBinder.bind(EditContentPage.class);
    }

    public void clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        searchButton.click();
    }

    public void clickJqlSearch()
    {
        Poller.waitUntilTrue(jqlSearch.timed().isEnabled());
        jqlSearch.click();
    }
}
