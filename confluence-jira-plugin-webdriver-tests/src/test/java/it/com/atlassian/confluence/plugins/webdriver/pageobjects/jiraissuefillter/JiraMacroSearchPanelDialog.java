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

    @Override
    public PageElement getPanelBodyDialog()
    {
        PageElement panelBodyDialog = find(CSS_SELECTOR_RECENT_PANEL);
        Poller.waitUntilTrue(panelBodyDialog.timed().isVisible());
        return panelBodyDialog;
    }

    public JiraMacroSearchPanelDialog clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        searchButton.click();
        return this;
    }

    public AbstractJiraIssueMacroDialog openDisplayOption()
    {
        Poller.waitUntilTrue(getPanelBodyDialog().find(By.id("jiraMacroDlg")).timed().isVisible());
        return super.openDisplayOption();
    }

    public TimedCondition hasInfoMessage(){
        PageElement infoMessage = getPanelBodyDialog().find(By.cssSelector(".aui-message.info"));
        return infoMessage.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible();
    }

    public String getInfoMessage()
    {
        return getPanelBodyDialog().find(By.cssSelector(".aui-message.info")).getText();
    }

    public PageElement getWarningMessageElement()
    {
        PageElement warningMessage = getPanelBodyDialog().find(By.cssSelector(".aui-message.warning"));
        Poller.waitUntilTrue(warningMessage.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible());

        return warningMessage;
    }
}
