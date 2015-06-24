package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;

import com.atlassian.pageobjects.elements.PageElement;

public class JiraMacroSearchPanelDialog extends AbstractJiraIssueFilterDialog
{
    protected static final String CSS_SELECTOR_RECENT_PANEL = "#my-recent-issues";

    @Override
    public PageElement getPanelBodyDialog()
    {
        return find(CSS_SELECTOR_RECENT_PANEL);
    }
}
