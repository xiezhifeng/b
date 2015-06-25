package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;

import com.atlassian.confluence.plugins.pageobjects.AbstractJiraIssueMacroDialog;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.Queries;
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
}
