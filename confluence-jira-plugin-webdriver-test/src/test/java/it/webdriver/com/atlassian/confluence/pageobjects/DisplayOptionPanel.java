package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import junit.framework.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import java.util.ArrayList;
import java.util.List;

public class DisplayOptionPanel extends ConfluenceAbstractPageComponent
{
    @ElementBy(cssSelector = "#jiraMacroDlg > .jql-display-opts-inner")
    private PageElement displayOptionsPanel;

    @ElementBy(id = "s2id_jiraIssueColumnSelector")
    private PageElement columnContainer;

    @ElementBy(cssSelector = ".select2-drop-multi")
    private PageElement columnDropDown;

    @ElementBy(cssSelector = ".select2-input")
    private PageElement select2Input;

    protected PageElement getRadioBtn(String value)
    {
        Poller.waitUntilTrue(displayOptionsPanel.timed().isEnabled());
        List<PageElement> elements = displayOptionsPanel.findAll(By.name("insert-advanced"));
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

    public void typeSelect2Input(String text)
    {
        Poller.waitUntilTrue(select2Input.timed().isVisible());
        select2Input.type(text);
    }

    public DisplayOptionPanel sendReturnKeyToAddedColoumn()
    {
        driver.findElement(By.cssSelector(".select2-input")).sendKeys(Keys.RETURN);
        return this;
    }

    public DisplayOptionPanel clickDisplaySingle()
    {
        //driver.findElement(By.xpath("//input[@value='insert-single']")).click();
        PageElement element = getRadioBtn("insert-single");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public DisplayOptionPanel clickDisplayTotalCount()
    {
        //driver.findElement(By.xpath("//input[@value='insert-count']")).click();
        PageElement element = getRadioBtn("insert-count");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
    }

    public DisplayOptionPanel clickDisplayTable()
    {
        //driver.findElement(By.xpath("//input[@value='insert-table']")).click();
        PageElement element = getRadioBtn("insert-table");
        Assert.assertNotNull("Cannot find proper radio button", element);
        element.click();
        return this;
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

    public DisplayOptionPanel addColumn(String... columnNames)
    {
        for (String columnName : columnNames)
        {
            clickSelected2Element();
            List<PageElement> options = this.columnDropDown.findAll(By.cssSelector(".select2-results > li"));
            for (PageElement option : options)
            {
                if(columnName.equals(option.getText()))
                {
                    option.click();
                    break;
                }
            }
            Poller.waitUntilTrue(columnContainer.timed().hasText(columnName));
        }

        return this;
    }

    public DisplayOptionPanel removeAllColumns()
    {
        List<String> selectedColumns = getSelectedColumns();
        for (String selectedColumn : selectedColumns)
        {
            removeSelectedColumn(selectedColumn);
        }

        return this;
    }

    public PageElement clickSelected2Element()
    {
        columnContainer.find(By.className("select2-choices")).click();
        return columnContainer;
    }

    public boolean isColumnsDisabled()
    {
        return columnContainer.hasClass("select2-container-disabled");
    }

    public boolean isInsertSingleIssueEnable()
    {
        PageElement element = getRadioBtn("insert-single");
        return element.isEnabled();
    }

    public boolean isInsertTableIssueEnable()
    {
        PageElement element = getRadioBtn("insert-table");
        return element.isEnabled();
    }

    public boolean isInsertCountIssueEnable()
    {
        PageElement element = getRadioBtn("insert-count");
        return element.isEnabled();
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

}
