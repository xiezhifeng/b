package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter;

import com.atlassian.pageobjects.elements.query.TimedCondition;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

import org.openqa.selenium.By;

public class JiraMacroSearchPanelDialog extends AbstractJiraIssueFilterDialog
{
    protected static final String CSS_SELECTOR_RECENT_PANEL = "#my-jira-search";

    /**
     * @deprecated Use {@link AbstractJiraIssueMacroDialog#getCurrentTabPanel()} instead.
     */
    @Deprecated
    @Override
    public PageElement getPanelBodyDialog()
    {
        PageElement panelBodyDialog = find(CSS_SELECTOR_RECENT_PANEL);
        Poller.waitUntilTrue(panelBodyDialog.timed().isVisible());
        return panelBodyDialog;
    }

    public AbstractJiraIssueMacroDialog openDisplayOption()
    {
        Poller.waitUntilTrue(getCurrentTabPanel().find(By.id("jiraMacroDlg")).timed().isVisible());
        return super.openDisplayOption();
    }

    public TimedCondition hasInfoMessage()
    {
        return getInfoMessageElement().withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible();
    }

    public String getInfoMessage()
    {
        return getInfoMessageElement().getText();
    }

    public PageElement getWarningMessageElement()
    {
        PageElement el = getCurrentTabPanel().find(By.cssSelector('#' + JIRA_DIALOG_2_ID + " " + CSS_SELECTOR_RECENT_PANEL + " .aui-message.warning"));
        Poller.waitUntilTrue(el.timed().isPresent());
        Poller.waitUntilTrue(el.timed().isVisible());
        return el;
    }

    public PageElement getInfoMessageElement()
    {
        PageElement el = getCurrentTabPanel().find(By.cssSelector("#" + JIRA_DIALOG_2_ID + " " + CSS_SELECTOR_RECENT_PANEL + " .aui-message.info"));
        Poller.waitUntilTrue(el.timed().isPresent());
        Poller.waitUntilTrue(el.timed().isVisible());
        return el;
    }
}
