package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.List;

import junit.framework.Assert;

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

    @ElementBy(className = ".jiraSearchResults")
    private PageElement issuesTable;

    @ElementBy(id = "s2id_jiraIssueColumnSelector")
    private PageElement columnContainer;
    
    @ElementBy(cssSelector = ".select2-drop-multi")
    private PageElement columnDropDown;
    
    @ElementBy(cssSelector = ".jql-display-opts-inner a")
    private PageElement displayOptBtn;

    @ElementBy(id = "jira-maximum-issues")
    private PageElement maxIssuesTxt;

    @ElementBy(cssSelector = "#jira-maximum-issues + #jira-max-number-error")
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
            driver.findElement(By.cssSelector("#jira-max-number-error.error"));
            return true;
        } catch (NoSuchElementException ex)
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
        //driver.findElement(By.xpath("//input[@value='insert-single']")).click();
        WebElement element = getRadioBtn("insert-single");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
    }

    public void clickDisplayTotalCount()
    {
        //driver.findElement(By.xpath("//input[@value='insert-count']")).click();
        WebElement element = getRadioBtn("insert-count");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
    }

    public void clickDisplayTable()
    {
        //driver.findElement(By.xpath("//input[@value='insert-table']")).click();
        WebElement element = getRadioBtn("insert-table");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
    }

    protected WebElement getRadioBtn(String value)
    {
        List<WebElement> elements = driver.findElements(By.name("insert-advanced"));
        Assert.assertEquals(3, elements.size());

        for (int i = 0; i < elements.size(); i++)
        {
            WebElement element = elements.get(i);
            String attr = element.getAttribute("value");
            if (value.equalsIgnoreCase(attr))
            {
                return element;
            }
        }

        return null;
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

    public PageElement getColumnContainer()
    {
        return columnContainer;
    }

    public PageElement getColumnDropDown()
    {
        return columnDropDown;
    }

    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-issue-button", false);
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
    
    public void clickSelected2Element()
    {
        this.columnContainer.find(By.className("select2-choices")).click();
    }
    
    public void cleanAllOptionColumn()
    {
        String script = "$('#jiraIssueColumnSelector').auiSelect2('val','');";
        driver.executeScript(script);
    }
    
    public void selectOption(String text)
    {
        List<PageElement> options = this.columnDropDown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if(text.equals(option.getText()))
            {
                option.click();
                break;
            }
        }
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

    public void openDisplayOption()
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
