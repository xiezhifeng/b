package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

public class JiraIssuesDialog extends Dialog
{

    @ElementBy(id = "macro-jira")
    private PageElement jiraMacroItem;

    @ElementBy(cssSelector = "#jira-connector .dialog-title")
    private PageElement dialogTitle;

    @ElementBy(cssSelector = "button[title='Search']")
    private PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    private PageElement jqlSearch;

    @ElementBy(className = ".jiraSearchResults")
    private PageElement issuesTable;

    @ElementBy(cssSelector = ".jql-display-opts-inner a")
    private PageElement displayOptBtn;

    @ElementBy(id = "jira-maximum-issues")
    private PageElement maxIssuesTxt;

    @ElementBy(cssSelector = "#jira-maximum-issues + #dialog-validation-error")
    private PageElement maxIssuesErrorMsg;

    public JiraIssuesDialog()
    {
        super("jira-connector");
    }

    public JiraIssuesDialog open()
    {
        jiraMacroItem.click();
        return this;
    }

    public String getTitleDialog()
    {
        return dialogTitle.getText();
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
        getMaxIssuesTxt().type(maxIssuesVal);

        // fire click to focusout the text box
        clickDisplayTable();
    }

    public boolean hasMaxIssuesErrorMsg()
    {
        try
        {
            driver.findElement(By.cssSelector("#jira-maximum-issues + #dialog-validation-error"));
            return true;
        }
        catch(NoSuchElementException ex)
        {
            return false;
        }
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

    public void clickDisplaySingle()
    {
        driver.findElement(By.xpath("//input[@value='insert-single']")).click();
    }

    public void clickDisplayTotalCount()
    {
        driver.findElement(By.xpath("//input[@value='insert-count']")).click();
    }

    public void clickDisplayTable()
    {
        driver.findElement(By.xpath("//input[@value='insert-table']")).click();
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
        clickButton("insert-issue-button", false);
        return pageBinder.bind(EditContentPage.class);
    }

    public void clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
    }

    public void clickJqlSearch()
    {
        Poller.waitUntilTrue(jqlSearch.timed().isEnabled());
        jqlSearch.click();
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

    protected void openDisplayOption()
    {
        Poller.waitUntilTrue(displayOptBtn.timed().isVisible());
        displayOptBtn.click();
    }

    protected void softCleanText(By by)
    {
        WebElement element = driver.findElement(by);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.CANCEL);
    }
}
