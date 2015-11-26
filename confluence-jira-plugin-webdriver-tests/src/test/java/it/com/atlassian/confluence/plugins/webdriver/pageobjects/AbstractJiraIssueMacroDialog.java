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
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.webdriver.utils.by.ByJquery;
import com.google.common.base.Supplier;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;

import java.util.List;

/**
 * Some methods in this class are belong to "com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraIssueFilterDialog".
 * They should be moved into correct place.
 */
public abstract class AbstractJiraIssueMacroDialog extends Dialog
{
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
        Poller.waitUntilTrue(getIssuesTable().timed().isPresent());
        getIssuesTable().find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).click();
        return this;
    }

    public AbstractJiraIssueMacroDialog clickSelectIssueOption(String key)
    {
        Poller.waitUntilTrue(getIssuesTable().timed().isPresent());
        getIssuesTable().find(ByJquery.$("input[type='checkbox'][value='" + key + "']")).click();
        return this;
    }

    public boolean isSelectAllIssueOptionChecked()
    {
        Poller.waitUntilTrue(getIssuesTable().timed().isPresent());
        return getIssuesTable().find(ByJquery.$("input[type='checkbox'][name='jira-issue-all']")).isSelected();
    }

    public TimedCondition isIssueExistInSearchResult(String issueKey)
    {
        return getIssuesTable().find(ByJquery.$("input[value='" + issueKey + "']")).timed().isVisible();
    }

    public PageElement getIssuesTable()
    {
        return getPanelBodyDialog().find(By.className("jiraSearchResults"));
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
        Poller.waitUntilTrue(openLink.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible());
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

    public TimedCondition resultsTableIsVisible()
    {
        return getIssuesTable().find(By.cssSelector(".my-result")).timed().isVisible();
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
