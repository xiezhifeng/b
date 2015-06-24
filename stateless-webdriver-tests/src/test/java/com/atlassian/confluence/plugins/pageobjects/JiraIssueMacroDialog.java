package com.atlassian.confluence.plugins.pageobjects;


import java.util.List;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedQuery;

import org.openqa.selenium.By;

public class JiraIssueMacroDialog extends Dialog
{
    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu")
    protected PageElement menu;

    @ElementBy(id = "jiralink")
    protected PageElement jiraMacroLink;

    @ElementBy(cssSelector = "#jira-connector .dialog-page-menu .selected")
    protected PageElement selectedMenu;

    @ElementBy(cssSelector = ".dialog-button-panel .insert-issue-button")
    protected PageElement insertButton;

    public JiraIssueMacroDialog(String id)
    {
        super(id);
    }

    public JiraIssueMacroDialog open()
    {
        jiraMacroLink.click();
        return this;
    }

    public void selectMenuItem(String menuItemText)
    {
        List<PageElement> menuItems = menu.findAll(By.tagName("button"));
        for(PageElement menuItem : menuItems)
        {
            if(menuItemText.equals(menuItem.getText()))
            {
                menuItem.click();
                break;
            }
        }
    }

    public void submit()
    {
        insertButton.click();
    }

    public PageElement getSelectedMenu()
    {
        return selectedMenu;
    }

   public TimedQuery<Boolean> isInsertButtonEnabled()
    {
        return insertButton.timed().isEnabled();
    }

}
