package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.List;

import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.webdriver.utils.by.ByJquery;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesDialog extends Dialog
{
    @ElementBy(id = "macro-jira")
    private PageElement jiraMacroItem;

    @ElementBy(cssSelector = "#jira-connector .dialog-title")
    private PageElement dialogTitle;

    @ElementBy(cssSelector = "#my-jira-search form button[title='Search']")
    private PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    private PageElement jqlSearch;

    @ElementBy(cssSelector = ".jiraSearchResults")
    private PageElement issuesTable;

    @ElementBy(cssSelector = ".jql-display-opts-inner a")
    private PageElement displayOptBtn;

    @ElementBy(id = "jira-maximum-issues")
    private PageElement maxIssuesTxt;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "#jira-connector .dialog-components .dialog-page-menu")
    private PageElement dialogMenu;

    @ElementBy(cssSelector = "#jira-connector .aui-message.warning")
    private PageElement warningMessage;

    @ElementBy(cssSelector = "#open-jira-chart-dialog")
    private PageElement jiraChartMacroAnchor;

    public JiraIssuesDialog()
    {
        super("jira-connector");
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public JiraIssuesDialog open()
    {
        jiraMacroItem.click();
        return this;
    }

    public String getDialogTitle()
    {
        return dialogTitle.getText();
    }

    public String getWarningMessage()
    {
        Poller.waitUntilTrue(warningMessage.timed().isVisible());
        return warningMessage.getText();
    }

    public void showDisplayOption()
    {
        String filterQuery = "status=open";
        inputJqlSearch(filterQuery);
        Poller.waitUntilTrue(getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(getSearchButton().timed().isEnabled());
        getSearchButton().click();

        openDisplayOption();
    }

    public void fillMaxIssues(String maxIssuesVal)
    {
        showDisplayOption();
        softCleanText(By.id("jira-maximum-issues"));
        getMaxIssuesTxt().clear().type(maxIssuesVal);

        // fire click to focusout the text box
        getDisplayOptionPanel().clickDisplayTable();
    }

    public boolean hasMaxIssuesErrorMsg()
    {
        try
        {
            driver.findElement(By.cssSelector("#jira-max-number-error.error"));
            return true;
        } catch (NoSuchElementException ex)
        {
            return false;
        }
    }

    public boolean isJqlSearchTextFocus()
    {
        return jqlSearch.getAttribute("name").equals(driver.switchTo().activeElement().getAttribute("name"));
    }

    public JiraIssuesDialog inputJqlSearch(String val)
    {
        Poller.waitUntilTrue(jqlSearch.timed().isVisible());
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }

    public JiraIssuesDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }

    public JiraIssuesDialog sendReturnKeyToJqlSearch()
    {
        driver.findElement(By.name("jiraSearch")).sendKeys(Keys.RETURN);
        return this;
    }

    public JiraIssuesDialog clickSelectAllIssueOption()
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        issuesTable.find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).click();
        return this;
    }

    public JiraIssuesDialog clickSelectIssueOption(String key)
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        issuesTable.find(ByJquery.$("input[type='checkbox'][value='" + key + "']")).click();
        return this;
    }

    public boolean isIssueExistInSearchResult(String issueKey)
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        return issuesTable.find(ByJquery.$("input[value='" + issueKey + "']")).isVisible();
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

    public DisplayOptionPanel getDisplayOptionPanel()
    {
        return pageBinder.bind(DisplayOptionPanel.class);
    }

    public PageElement getInsertButton()
    {
        return insertButton;
    }

    public EditContentPage clickInsertDialog()
    {
        Poller.waitUntilTrue(insertButton.timed().isEnabled());
        clickButton("insert-issue-button", false);
        return pageBinder.bind(EditContentPage.class);
    }

    public JiraIssuesDialog clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        searchButton.click();
        return this;
    }

    public JiraIssuesDialog clickJqlSearch()
    {
        Poller.waitUntilTrue(jqlSearch.timed().isEnabled());
        jqlSearch.click();
        return this;
    }

    public JiraIssuesDialog cleanAllOptionColumn()
    {
        String script = "$('#jiraIssueColumnSelector').auiSelect2('val','');";
        driver.executeScript(script);
        return this;
    }

    public List<PageElement> insertAndSave()
    {
        EditContentPage editContentPage = clickInsertDialog();
        ViewPage viewPage = editContentPage.save();
        return viewPage.getMainContent().findAll(By.cssSelector("table.aui tr.rowNormal"));
    }

    public PageElement getMaxIssuesTxt()
    {
        return maxIssuesTxt;
    }

    public void setMaxIssuesTxt(PageElement maxIssuesTxt)
    {
        this.maxIssuesTxt = maxIssuesTxt;
    }

    public JiraIssuesDialog openDisplayOption()
    {
        Poller.waitUntilTrue(displayOptBtn.timed().isVisible());
        displayOptBtn.click();
        return this;
    }

    public void uncheckKey(String key)
    {
        PageElement checkbox = getJiraIssuesCheckBox(key);
        Poller.waitUntilTrue(checkbox.timed().isVisible());
        checkbox.click();
    }

    public PageElement getJiraIssuesCheckBox(String key)
    {
        return pageElementFinder.find(By.cssSelector(".issue-checkbox-column input[value='" + key + "']"));
    }

    public boolean isInsertable()
    {
        return insertButton.isEnabled();
    }



    public void selectMenuItem(int index)
    {
        Poller.waitUntilTrue(dialogMenu.timed().isVisible());
        dialogMenu.find(By.cssSelector("li.page-menu-item:nth-child(" + index + ") > button.item-button")).click();
    }

    protected void softCleanText(By by)
    {
        WebElement element = driver.findElement(by);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.CANCEL);
    }

    public TimedCondition resultsTableIsVisible()
    {
        return issuesTable.find(By.cssSelector(".my-result")).timed().isVisible();
    }

    public PageElement getJiraChartMacroAnchor()
    {
        return jiraChartMacroAnchor;
    }

    public void clickJiraChartMacroAnchor()
    {
        jiraChartMacroAnchor.click();
    }
}
