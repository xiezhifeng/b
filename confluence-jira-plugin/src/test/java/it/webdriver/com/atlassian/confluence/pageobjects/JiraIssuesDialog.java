package it.webdriver.com.atlassian.confluence.pageobjects;

import java.util.List;

import org.openqa.selenium.By;

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

    @ElementBy(cssSelector = "#my-jira-search form button[title='Search']")
    private PageElement searchButton;

    @ElementBy(name = "jiraSearch")
    private PageElement jqlSearch;

    @ElementBy(className = ".jiraSearchResults")
    private PageElement issuesTable;
    
    @ElementBy(cssSelector = ".jql-display-opts-inner a")
    private PageElement displayOptBtn;
    
    @ElementBy(id = "s2id_jiraIssueColumnSelector")
    private PageElement columnContainer;
    
    @ElementBy(cssSelector = ".select2-drop-multi")
    private PageElement columnDropDown;
    
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

    public PageElement getIssuesTable()
    {
        return issuesTable;
    }

    public PageElement getColumnContainer()
    {
        return columnContainer;
    }

    public PageElement getColumnDropDown()
    {
        return columnDropDown;
    }

    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-issue-button", false);
        return pageBinder.bind(EditContentPage.class);
    }

    public void clickSearchButton()
    {
        Poller.waitUntilTrue(searchButton.timed().isVisible());
        searchButton.click();
    }

    public void clickJqlSearch()
    {
        Poller.waitUntilTrue(jqlSearch.timed().isEnabled());
        jqlSearch.click();
    }
    
    public void openDisplayOption()
    {
        Poller.waitUntilTrue(displayOptBtn.timed().isVisible());
        displayOptBtn.click();
    }
    
    public void clickSelected2Element(PageElement containerElement)
    {
        containerElement.find(By.className("select2-choices")).click();
    }
    
    public void cleanAllOptionColumn()
    {
        String script = "$('#jiraIssueColumnSelector').auiSelect2('val','');";
        driver.executeScript(script);
    }
    
    public void selectOption(PageElement selectDropDown, String text)
    {
        List<PageElement> options = selectDropDown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if(text.equals(option.getText()))
            {
                option.click();
                break;
            }
        }
    }
}
