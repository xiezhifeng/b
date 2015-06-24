package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

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
}
