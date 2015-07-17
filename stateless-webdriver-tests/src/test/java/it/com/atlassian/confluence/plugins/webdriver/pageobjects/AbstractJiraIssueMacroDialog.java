package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.Queries;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.webdriver.utils.by.ByJquery;
import com.google.common.base.Supplier;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Some methods in this class are belong to "com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraIssueFilterDialog".
 * They should be moved into correct place.
 */
public abstract class AbstractJiraIssueMacroDialog extends Dialog
{
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

    @ElementBy(cssSelector = ".jiraSearchResults")
    protected PageElement issuesTable;

    public AbstractJiraIssueMacroDialog(String id)
    {
        super(id);
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public void selectMenuItem(String menuItemText)
    {
        List<PageElement> menuItems = getDialogMenu().findAll(By.tagName("button"));
        for(PageElement menuItem : menuItems)
        {
            if(menuItemText.equals(menuItem.getText()))
            {
                menuItem.click();
                break;
            }
        }
    }

    public PageElement getSelectedMenu()
    {
        return getDialogMenu().find(By.cssSelector(".selected"));
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

    public void inputJqlSearch(String val)
    {
        PageElement jqlSearch = getJqlSearchElement();
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
    }

    public void pasteJqlSearch(String val)
    {
        PageElement jqlSearch = getJqlSearchElement();
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
    }

    public PageElement getJqlSearchElement()
    {
        PageElement pageElement = getPanelBodyDialog().find(By.name("jiraSearch"));
        Poller.waitUntilTrue(pageElement.timed().isVisible());
        return pageElement;
    }

    public void clickJqlSearch()
    {
        getJqlSearchElement().click();
    }

    public void sendReturnKeyToJqlSearch()
    {
        PageElement jqlSearch = getJqlSearchElement();
        jqlSearch.type(Keys.RETURN);
    }

    public AbstractJiraIssueMacroDialog clickSelectAllIssueOption()
    {
        Poller.waitUntilTrue(issuesTable.timed().isPresent());
        issuesTable.find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).click();
        return this;
    }

    public AbstractJiraIssueMacroDialog clickSelectIssueOption(String key)
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

    public PageElement getIssuesTable()
    {
        return issuesTable;
    }

    public DisplayOptionPanel getDisplayOptionPanel()
    {
        return pageBinder.bind(DisplayOptionPanel.class);
    }

    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-issue-button", false);
        waitUntilHidden();
        return pageBinder.bind(EditContentPage.class);
    }

    public AbstractJiraIssueMacroDialog cleanAllOptionColumn()
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

    /**
     * Child dialog must override its 'getPanelBodyDialog' method.
     */
    public AbstractJiraIssueMacroDialog openDisplayOption()
    {
        PageElement openLink = getPanelBodyDialog().find(By.cssSelector("[data-js=\"display-option-trigger\"]"));
        if (openLink.isPresent() && openLink.isVisible())
        {
            openLink.click();
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
                return getPanelBodyDialog().find(By.cssSelector("[data-js=\"display-option-wrapper\"]"))
                        .javascript().execute("return jQuery(arguments[0]).css(\"bottom\")").equals("0px");
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

    public void selectMenuItem(int index)
    {
        Poller.waitUntilTrue(getDialogMenu().timed().isVisible());
        getDialogMenu().find(By.cssSelector("li.page-menu-item:nth-child(" + index + ") > button.item-button")).click();
    }

    public PageElement getDialogMenu()
    {
        return find(".dialog-page-menu");
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

    public abstract PageElement getPanelBodyDialog();

    public PageElement queryPageElement(String cssSelector)
    {
        PageElement pageElement = getPanelBodyDialog().find(By.cssSelector(cssSelector));
        Poller.waitUntilTrue(pageElement.timed().isVisible());
        return pageElement;
    }

    public void triggerChangeEvent(PageElement element)
    {
        element.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
    }
}
