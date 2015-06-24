package com.atlassian.confluence.plugins.pageobjects;


import java.util.List;
import java.util.concurrent.TimeUnit;

import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.Queries;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.webdriver.utils.by.ByJquery;

import com.google.common.base.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.apache.commons.lang3.StringUtils.split;

public class JiraIssueMacroDialog extends Dialog
{
    @ElementBy(cssSelector = ".dialog-page-menu")
    protected PageElement menu;

    @ElementBy(id = "jiralink")
    protected PageElement jiraMacroLink;

    @ElementBy(cssSelector = ".dialog-page-menu .selected")
    protected PageElement selectedMenu;

    @ElementBy(cssSelector = "#main-content table.aui .jira-tablesorter-header")
    protected PageElement headerIssueTable;

    @ElementBy(cssSelector = "#main-content .icon-refresh")
    protected PageElement refreshedIcon;

    @ElementBy(cssSelector = "#main-content .static-jira-issues_count > .issue-link")
    protected PageElement issuesCount;

    @ElementBy(cssSelector = ".refresh-issues-bottom [id^=total-issues-count] a")
    protected PageElement issuesTableRowCount;

    @ElementBy(id = "main-content")
    protected PageElement main;

    @ElementBy(cssSelector = ".jim-sortable-dark-layout")
    protected PageElement sortableDarkLayout;

    @ElementBy(cssSelector = ".jira-issue")
    protected PageElement singleJiraIssue;

    @ElementBy(cssSelector = ".jiraissues_table .flexigrid")
    protected PageElement dynamicJiraIssueTable;

    @ElementBy(cssSelector = ".jim-error-message a")
    protected PageElement jiraErrorLink;

    @ElementBy(cssSelector = ".jim-error-message")
    protected PageElement jiraErrorMessage;

     @ElementBy(id = "macro-jira")
    protected PageElement jiraMacroItem;

    @ElementBy(cssSelector = ".dialog-title")
    protected PageElement dialogTitle;

    @ElementBy(cssSelector = "#my-jira-search form button[title='Search']")
    protected PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    protected PageElement jqlSearch;

    @ElementBy(cssSelector = ".jiraSearchResults")
    protected PageElement issuesTable;

    @ElementBy(id = "jira-maximum-issues")
    protected PageElement maxIssuesTxt;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    protected PageElement insertButton;

    @ElementBy(cssSelector = ".dialog-components .dialog-page-menu")
    protected PageElement dialogMenu;

    @ElementBy(cssSelector = ".aui-message.warning")
    protected PageElement warningMessage;

    @ElementBy(cssSelector = "#open-jira-chart-dialog")
    protected PageElement jiraChartMacroAnchor;

    @ElementBy(cssSelector = ".aui-message.info", timeoutType = TimeoutType.PAGE_LOAD)
    protected PageElement infoMessage;

    public JiraIssueMacroDialog(String id)
    {
        super(id);
    }

    public JiraIssueMacroDialog open()
    {
        jiraMacroLink.click();
        return this;
    }
    
    @Init
    public void bind()
    {
        waitUntilVisible();
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

    public void submit()
    {
        insertButton.click();
    }

    public PageElement getSelectedMenu()
    {
        return selectedMenu;
    }

    public TimedQuery<Boolean> isInsertButtonEnabled()
    {
        return insertButton.timed().isEnabled();
    }
    
    public int getIssueCount()
    {
        return getIssuesCountFromText(issuesCount.getText());
    }

    public int getNumberOfIssuesInTable()
    {
        return getIssuesCountFromText(getNumberOfIssuesText());
    }

    public String getNumberOfIssuesText()
    {
        waitUntilFalse(sortableDarkLayout.timed().isVisible());
        return issuesTableRowCount.getText();
    }

    public void clickRefreshedIcon()
    {
        refreshedIcon.click();
    }

    protected int getIssuesCountFromText(String text)
    {
        return Integer.parseInt(split(text, " ")[0]);
    }
    
    public void clickColumnHeaderIssueTable(String columnName)
    {
        
        List<PageElement> columns = getIssuesTableColumns();
        for (PageElement column : columns)
        {
            if (column.find(By.cssSelector(".jim-table-header-content")).getText().trim().equalsIgnoreCase(columnName))
            {
                column.click();
                break;
            }
        }
    }

    public PageElement getDynamicJiraIssueTable() {
        return dynamicJiraIssueTable;
    }

    public List<PageElement> getIssuesTableColumns()
    {
        return issuesTable.findAll(By.cssSelector(".jira-tablesorter-header"));
    }

    public String getFirstRowValueOfSummay() 
    {
        waitUntilTrue("JIRA issues table is not visible", issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[2]/a")).getText();
    }

    public boolean isSingleContainText(String text)
    {
        waitUntilTrue("Single JIRA issue is not visible", singleJiraIssue.timed().isVisible());
        return singleJiraIssue.getText().contains(text);
    }

    public PageElement getIssuesTableElement()
    {
        return issuesTable;
    }

    public PageElement getIssuesCountElement()
    {
        return issuesCount;
    }

    public PageElement getRefreshedIconElement()
    {
        return refreshedIcon;
    }

    public PageElement getJiraErrorLink()
    {
        waitUntilTrue(jiraErrorLink.timed().isVisible());
        return jiraErrorLink;
    }

    public PageElement getErrorMessage()
    {
        waitUntilTrue(jiraErrorMessage.timed().isVisible());
        return jiraErrorMessage;
    }

    public String getFirstRowValueOfAssignee()
    {
        waitUntilTrue(issuesTable.timed().isPresent());
        return main.find(By.xpath("//table[@class='aui']/tbody/tr[3]/td[7]")).getText();
    }

    public PageElement getDialogTitle()
    {
        return dialogTitle;
    }

    public String getWarningMessage()
    {
        Poller.waitUntilTrue(warningMessage.timed().isVisible());
        return warningMessage.getText();
    }

    public String getInfoMessage()
    {
        Poller.waitUntilTrue(infoMessage.timed().isVisible());
        return infoMessage.getText();
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

    public JiraIssueMacroDialog inputJqlSearch(String val)
    {
        Poller.waitUntilTrue(jqlSearch.timed().isVisible());
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }

    public JiraIssueMacroDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }

    public JiraIssueMacroDialog sendReturnKeyToJqlSearch()
    {
        driver.findElement(By.name("jiraSearch")).sendKeys(Keys.RETURN);
        return this;
    }

    public JiraIssueMacroDialog clickSelectAllIssueOption()
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        issuesTable.find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).click();
        return this;
    }

    public JiraIssueMacroDialog clickSelectIssueOption(String key)
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        issuesTable.find(ByJquery.$("input[type='checkbox'][value='" + key + "']")).click();
        return this;
    }

    public boolean isSelectAllIssueOptionChecked()
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        return issuesTable.find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).isSelected();
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
        clickButton("insert-issue-button", false);
        return pageBinder.bind(EditContentPage.class);
    }

    public JiraIssueMacroDialog clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        searchButton.click();
        return this;
    }

    public JiraIssueMacroDialog clickJqlSearch()
    {
        Poller.waitUntilTrue(jqlSearch.timed().isEnabled());
        jqlSearch.click();
        return this;
    }

    public JiraIssueMacroDialog cleanAllOptionColumn()
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

    public JiraIssueMacroDialog openDisplayOption()
    {
        PageElement openLink = find(".jirachart-display-opts-open");
        if (openLink.isPresent() && openLink.isVisible())
        {
            openLink.click();
            Poller.waitUntilTrue(find(".jirachart-display-opts-close").timed().isVisible());
            //TODO: need to fix this hacky code
            Poller.waitUntilTrue(Queries.forSupplier(timeouts, hasShowingDisplayOptionFull()));
        }

        return this;
    }

    protected Supplier<Boolean> hasShowingDisplayOptionFull()
    {
        return new Supplier<Boolean>()
        {
            @Override
            public Boolean get()
            {
                return JiraIssueMacroDialog.this.find(".jira-chart-option").getAttribute("style").contains("bottom");
            }
        };

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

    public PieChartDialog clickJiraChartMacroAnchor()
    {
        jiraChartMacroAnchor.click();
        PieChartDialog pieChartDialog = this.pageBinder.bind(PieChartDialog.class);
        Poller.waitUntilTrue(pieChartDialog.isVisibleTimed());
        return pieChartDialog;
    }
}
