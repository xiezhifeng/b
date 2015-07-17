package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter;

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
        return find(CSS_SELECTOR_RECENT_PANEL);
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

    public String getInfoMessage()
    {
        PageElement infoMessage = getInfoMessageElement();
        return infoMessage.getText();
    }

    public PageElement getInfoMessageElement()
    {
        PageElement infoMessage = getPanelBodyDialog().find(By.cssSelector(".aui-message.info"));
        Poller.waitUntilTrue(infoMessage.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible());
        return infoMessage;
    }

    public String getWarningMessage()
    {
        PageElement warningMessage = getWarningMessageElement();
        return warningMessage.getText();
    }

    public PageElement getWarningMessageElement()
    {
        PageElement warningMessage = getPanelBodyDialog().find(By.cssSelector(".aui-message.warning"));
        Poller.waitUntilTrue(warningMessage.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().isVisible());

        return warningMessage;
    }
}
