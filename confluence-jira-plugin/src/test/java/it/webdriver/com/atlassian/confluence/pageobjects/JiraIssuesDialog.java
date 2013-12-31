package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.ArrayList;
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

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    private PageElement insertButton;

    @ElementBy(cssSelector = "#jiraMacroDlg > .jql-display-opts-inner")
    private PageElement jqlDisplayOptionsPanel;

    @ElementBy(cssSelector = "#jira-connector .dialog-components .dialog-page-menu")
    private PageElement dialogMenu;

    @ElementBy(cssSelector = "#create-issues-form .project-select-parent .project-select")
    private PageElement projectSelect;

    @ElementBy(cssSelector = "#create-issues-form .type-select-parent .type-select")
    private PageElement issueTypeSelect;

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
        getMaxIssuesTxt().clear().type(maxIssuesVal);

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

    public JiraIssuesDialog clickDisplaySingle()
    {
        //driver.findElement(By.xpath("//input[@value='insert-single']")).click();
        WebElement element = getRadioBtn("insert-single");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public JiraIssuesDialog clickDisplayTotalCount()
    {
        //driver.findElement(By.xpath("//input[@value='insert-count']")).click();
        WebElement element = getRadioBtn("insert-count");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public JiraIssuesDialog clickDisplayTable()
    {
        //driver.findElement(By.xpath("//input[@value='insert-table']")).click();
        WebElement element = getRadioBtn("insert-table");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    protected WebElement getRadioBtn(String value)
    {
        Poller.waitUntilTrue(getJqlDisplayOptionsPanel().timed().isEnabled());
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

    public PageElement getJqlDisplayOptionsPanel()
    {
        return jqlDisplayOptionsPanel;
    }

    public PageElement getProjectSelect()
    {
        return projectSelect;
    }

    public PageElement getIssueTypeSelect()
    {
        return issueTypeSelect;
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
    
    public JiraIssuesDialog clickSelected2Element()
    {
        this.columnContainer.find(By.className("select2-choices")).click();
        return this;
    }
    
    public JiraIssuesDialog cleanAllOptionColumn()
    {
        String script = "$('#jiraIssueColumnSelector').auiSelect2('val','');";
        driver.executeScript(script);
        return this;
    }

    public List<String> getSelectedColumns()
    {
        List<PageElement> selectedColumns = columnContainer.findAll(By.cssSelector(".select2-choices .select2-search-choice"));
        List<String> selectedColumnNames = new ArrayList<String>();
        for (PageElement selectedColumn :  selectedColumns)
        {
            selectedColumnNames.add(selectedColumn.getText());
        }
        return selectedColumnNames;
    }

    public void removeSelectedColumn(String columnName)
    {
        PageElement removeColumn = getSelectedColumn(columnName);
        if(removeColumn != null)
        {
            PageElement closeButton = removeColumn.find(By.cssSelector(".select2-search-choice-close"));
            closeButton.click();
        }
        Poller.waitUntilFalse(columnContainer.timed().hasText(columnName));
    }

    public JiraIssuesDialog addColumn(String columnName)
    {
        clickSelected2Element();
        List<PageElement> options = this.columnDropDown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if(columnName.equals(option.getText()))
            {
                option.click();
                break;
            }
        }
        Poller.waitUntilTrue(columnContainer.timed().hasText(columnName));
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
        getJiraIssuesCheckBox(key).click();
    }

    public PageElement getJiraIssuesCheckBox(String key)
    {
        return pageElementFinder.find(By.cssSelector(".issue-checkbox-column input[value='" + key + "']"));
    }

    public boolean isInsertable()
    {
        return insertButton.isEnabled();
    }

    public boolean isColumnsDisabled()
    {
        return columnContainer.hasClass("select2-container-disabled");
    }

    public void selectMenuItem(int index)
    {
        Poller.waitUntilTrue(dialogMenu.timed().isPresent());
        dialogMenu.find(By.cssSelector("li.page-menu-item:nth-child(" + index + ") > button.item-button")).click();
    }

    protected void softCleanText(By by)
    {
        WebElement element = driver.findElement(by);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.CANCEL);
    }

    private PageElement getSelectedColumn(String columnName) 
    {
        List<PageElement> selectedColumns = columnContainer.findAll(By.cssSelector(".select2-choices .select2-search-choice"));
        for (PageElement selectedColumn :  selectedColumns)
        {
            if(columnName.equals(selectedColumn.getText()))
            {
                return selectedColumn;
            }
        }
        return null;
    }
}
