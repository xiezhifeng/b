package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;


import com.atlassian.confluence.plugins.pageobjects.AbstractJiraIssueMacroDialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedQuery;

public abstract class AbstractJiraIssueFilterDialog extends AbstractJiraIssueMacroDialog
{
    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    protected PageElement insertButton;

    public AbstractJiraIssueFilterDialog()
    {
        super("jira-connector");
    }

    public void submit()
    {
        insertButton.click();
    }

    public TimedQuery<Boolean> isInsertButtonEnabled()
    {
        return insertButton.timed().isEnabled();
}

    public PageElement getInsertButton()
    {
        return insertButton;
    }

}
