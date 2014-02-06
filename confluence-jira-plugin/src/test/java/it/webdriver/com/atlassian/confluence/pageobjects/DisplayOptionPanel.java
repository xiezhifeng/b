package it.webdriver.com.atlassian.confluence.pageobjects;


import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import junit.framework.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import java.util.ArrayList;
import java.util.List;

public class DisplayOptionPanel
{
    @ElementBy(cssSelector = "#jiraMacroDlg > .jql-display-opts-inner")
    private PageElement jqlDisplayOptionsPanel;

    @ElementBy(id = "jira-maximum-issues")
    private PageElement maxIssues;

    @ElementBy(id = "s2id_jiraIssueColumnSelector")
    private PageElement columnContainer;

    @ElementBy(cssSelector = ".select2-drop-multi")
    private PageElement columnDropDown;

    protected PageElement getRadioBtn(String value)
    {
        Poller.waitUntilTrue(jqlDisplayOptionsPanel.timed().isEnabled());
        List<PageElement> elements = jqlDisplayOptionsPanel.findAll(By.name("insert-advanced"));
        Assert.assertEquals(3, elements.size());

        for (PageElement element : elements)
        {
            String attr = element.getAttribute("value");
            if (value.equalsIgnoreCase(attr))
            {
                return element;
            }
        }

        return null;
    }

    public DisplayOptionPanel clickDisplaySingle()
    {
        PageElement element = getRadioBtn("insert-single");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public DisplayOptionPanel clickDisplayTotalCount()
    {
        PageElement element = getRadioBtn("insert-count");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public DisplayOptionPanel clickDisplayTable()
    {
        PageElement element = getRadioBtn("insert-table");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public boolean isInsertSingleEnable()
    {
        PageElement element = getRadioBtn("insert-single");
        return element.isEnabled();
    }

    public boolean isInsertTableEnable()
    {
        PageElement element = getRadioBtn("insert-table");
        return element.isEnabled();
    }

    public boolean isInsertCountEnable()
    {
        PageElement element = getRadioBtn("insert-count");
        return element.isEnabled();
    }

    public boolean hasMaxIssuesErrorMsg()
    {
        try
        {
            jqlDisplayOptionsPanel.find(By.cssSelector("#jira-max-number-error.error"));

            return true;
        }
        catch (NoSuchElementException ex)
        {
            return false;
        }
    }

    public DisplayOptionPanel clickSelected2Element()
    {
        columnContainer.find(By.className("select2-choices")).click();
        return this;
    }

    public boolean isColumnsDisabled()
    {
        return columnContainer.hasClass("select2-container-disabled");
    }

    public List<String> getSelectedColumns()
    {
        List<PageElement> selectedColumns = columnContainer.findAll(By.cssSelector(".select2-choices .select2-search-choice"));
        List<String> selectedColumnNames = new ArrayList<String>();
        for (PageElement selectedColumn :  selectedColumns)
        {
            selectedColumnNames.add(selectedColumn.getText());
        }
        return selectedColumnNames;
    }

    public DisplayOptionPanel addColumn(String columnName)
    {
        clickSelected2Element();
        List<PageElement> options = columnDropDown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if(columnName.equals(option.getText()))
            {
                option.click();
                break;
            }
        }
        Poller.waitUntilTrue(columnContainer.timed().hasText(columnName));
        return this;
    }

    public void removeSelectedColumn(String columnName)
    {
        PageElement removeColumn = getSelectedColumn(columnName);
        if(removeColumn != null)
        {
            PageElement closeButton = removeColumn.find(By.cssSelector(".select2-search-choice-close"));
            closeButton.click();
        }
        Poller.waitUntilFalse(columnContainer.timed().hasText(columnName));
    }

    private PageElement getSelectedColumn(String columnName)
    {
        List<PageElement> selectedColumns = columnContainer.findAll(By.cssSelector(".select2-choices .select2-search-choice"));
        for (PageElement selectedColumn :  selectedColumns)
        {
            if(columnName.equals(selectedColumn.getText()))
            {
                return selectedColumn;
            }
        }
        return null;
    }

    public PageElement getMaxIssues()
    {
        return maxIssues;
    }

    public PageElement getColumnContainer()
    {
        return columnContainer;
    }

    public PageElement getColumnDropDown()
    {
        return columnDropDown;
    }
}
