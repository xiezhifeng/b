package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import java.util.List;

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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

/**
 * Some methods in this class are belong to "com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraIssueFilterDialog".
 * They should be moved into correct place.
 */
public abstract class AbstractJiraIssueMacroDialog extends Dialog
{
    public static final String JIRA_DIALOG_2_ID = "jira-issue-dialog";
    public static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    public static final String OAUTH_LOGIN_JIRA_URL = "/jira/login.jsp?permissionViolation=true";

    @ElementBy(cssSelector = ".jiraSearchResults")
    protected PageElement issuesTable;

    @ElementBy(cssSelector = "#" + JIRA_DIALOG_2_ID + " .dialog-submit-button")
    protected PageElement insertButton;

    public AbstractJiraIssueMacroDialog()
    {
        super(JIRA_DIALOG_2_ID);
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public void waitUntilVisible()
    {
        super.waitUntilVisible();
        Poller.waitUntilFalse(getDialog().timed().hasClass("loading"));
    }

    /**
     * Select a tab by tab name
     * @param tabText
     */
    public void selectTabItem(String tabText)
    {
        List<PageElement> tabAnchors = getDialogTabsOfCurrentPanel().findAll(By.tagName("a"));

        for(PageElement anchor : tabAnchors)
        {
            if(tabText.equals(anchor.getText()))
            {
                anchor.click();
                Poller.waitUntil(anchor.timed().getAttribute("aria-selected"), Matchers.equalToIgnoringCase("true"));
                break;
            }
        }
    }

    /**
     * Get selected tab title
     */
    public String getSelectedTabItem()
    {
        List<PageElement> tabs = getDialogTabsOfCurrentPanel().findAll(By.tagName("li"));

        for(PageElement tab : tabs)
        {
            if(tab.hasClass("active-tab"))
            {
                return tab.getText();
            }
        }

        return "";
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

    public TimedCondition isIssueExistInSearchResult(String issueKey)
    {
        return issuesTable.find(ByJquery.$("input[value='" + issueKey + "']")).timed().isVisible();
    }

    public PageElement getIssuesTable()
    {
        return issuesTable;
    }

    public DisplayOptionPanel getDisplayOptionPanel()
    {
        return pageBinder.bind(DisplayOptionPanel.class);
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

    /**
     * Switching between tabs by tab index.
     * @param {Integer} index
     */
    public void selectTabItem(int index)
    {
        Poller.waitUntilTrue(getDialogTabsOfCurrentPanel().timed().isVisible());
        getDialogTabsOfCurrentPanel().find(By.cssSelector("li.menu-item:nth-child(" + index + ") > a")).click();
    }

    public PageElement getDialogTabsOfCurrentPanel()
    {
        return getContainerOfSelectedPanel().find(By.cssSelector(".tabs-menu"));
    }

    public TimedCondition resultsTableIsVisible()
    {
        return issuesTable.find(By.cssSelector(".my-result")).timed().isVisible();
    }

    public abstract PageElement getPanelBodyDialog();

    /**
     * Get current active tab panel
     * @return PageElement
     */
    public PageElement getCurrentTabPanel()
    {
        return getContainerOfSelectedPanel().find(By.cssSelector(".tabs-pane.active-pane"));
    }

    /**
     * Get current selected panel container
     * @return PageElement
     */
    public PageElement getContainerOfSelectedPanel()
    {
        PageElement selectedPanel = find(".page-menu-item.selected");
        String selectedPanelId = selectedPanel.getAttribute("data-panel-id");
        return find(".dialog-main-content-inner." + selectedPanelId);
    }

    /**
     * Select a panel by panel name
     */
    public void selectPanel(String panelName)
    {
        List<PageElement> panels = findAll(".dialog-page-menu .page-menu-item");

        for(PageElement panel : panels)
        {
            if(panelName.equals(panel.getText()))
            {
                panel.click();
                Poller.waitUntilTrue(panel.timed().hasClass("selected"));
                break;
            }
        }
    }

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

    /**
     *
     * Close dialog by clicking cancel button.
     * Because we apply Dialog2, we have to override "clickCancel" method of parent class.
     */
    public void clickCancel()
    {
        find(".dialog-close-button").click();
        waitUntilHidden();
    }

    /**
     * Close dialog by clicking Insert button.
     * @return EditContentPage
     */
    public EditContentPage clickInsertDialog()
    {
        waitUntilTrue(insertButton.timed().isEnabled());
        waitUntil(insertButton.timed().getAttribute("aria-disabled"), Matchers.equalToIgnoringCase("false"));
        insertButton.click();
        return pageBinder.bind(EditContentPage.class);
    }

    public TimedQuery<Boolean> isInsertButtonEnabledTimed()
    {
        return insertButton.timed().isEnabled();
    }

    /**
     * Check whether it is necessary to do authentication or not
     * @return Boolean
     */
    public boolean needAuthentication()
    {
        PageElement pageElement = getAuthenticationLink();
        return pageElement.isPresent() && pageElement.isVisible();
    }

    public PageElement getAuthenticationLink()
    {
        return getCurrentTabPanel().find(By.className("oauth-init"));
    }

    /**
     *  Do login if we have Un-trust AppLink
     */
    public void doOAuthenticate()
    {
        getAuthenticationLink().click();

        boolean isAuthenticateSuccess = false;
        //before any pop ups are open
        String parentHandle = driver.getWindowHandle();
        //after you have pop ups
        for (String popUpHandle : driver.getWindowHandles())
        {
            if(!popUpHandle.equals(parentHandle)){
                driver.switchTo().window(popUpHandle);
                // finding oauthentication page. Note we must login with JIRA first
                if(driver.getCurrentUrl().contains(OAUTH_URL) || driver.getCurrentUrl().contains(OAUTH_LOGIN_JIRA_URL))
                {
                    JiraAuthenticationPage authenticationPage = pageBinder.bind(JiraAuthenticationPage.class);
                    isAuthenticateSuccess = authenticationPage.doApprove();
                }
            }
        }

        Assert.assertTrue("Authenticate application link", isAuthenticateSuccess);

        // switch back to main page
        driver.switchTo().window(parentHandle);
    }

    public void waitUntilNoSpinner()
    {
        PageElement spinner = getContainerOfSelectedPanel().find(By.cssSelector(".loading-data .spinner"));
        if (spinner.isPresent())
        {
            waitUntilFalse(spinner.timed().isPresent());
            waitUntilFalse(spinner.timed().isVisible());
        }
    }
}
