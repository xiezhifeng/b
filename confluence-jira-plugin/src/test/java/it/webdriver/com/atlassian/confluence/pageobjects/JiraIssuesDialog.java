package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesDialog extends Dialog
{

    @ElementBy(id = "macro-jira")
    private PageElement jiraMacroItem;

    @ElementBy(cssSelector = "#jira-connector .dialog-title")
    private PageElement dialogTitle;

    @ElementBy(cssSelector = "button[title='Search']")
    private PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    private PageElement jqlSearch;

    @ElementBy(className = ".jiraSearchResults")
    private PageElement issuesTable;

    public JiraIssuesDialog()
    {
        super("jira-connector");
    }

    public JiraIssuesDialog open()
    {
        jiraMacroItem.click();
        return this;
    }

    public String getTitleDialog()
    {
        return dialogTitle.getText();
    }

    public JiraIssuesDialog inputJqlSearch(String val)
    {
        Poller.waitUntilTrue(jqlSearch.timed().isVisible());
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }

    public JiraIssuesDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }
    
    public String getJqlSearch()
    {
        return jqlSearch.getValue();
    }

    public PageElement getJQLSearchElement()
    {
        return jqlSearch;
    }

    public PageElement getSearchButton()
    {
        return searchButton;
    }

    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-issue-button", false);
        return pageBinder.bind(EditContentPage.class);
    }

    public void clickSearchButton() {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
    }

}
