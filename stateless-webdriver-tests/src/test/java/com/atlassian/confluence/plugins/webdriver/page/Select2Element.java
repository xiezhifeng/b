package com.atlassian.confluence.plugins.webdriver.page;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;

public class Select2Element extends ConfluenceAbstractPageComponent
{
    @ElementBy(id = "select2-drop", timeoutType = PAGE_LOAD)
    protected PageElement select2Dropdown;

    private PageElement selectElement;

    @Deprecated
    public Select2Element()
    {
        // default c'tor
    }

    public Select2Element(PageElement selectElement)
    {
        this.selectElement = selectElement;
    }

    @Deprecated
    public void bindingElements(PageElement selectElement)
    {
        this.selectElement = selectElement;
    }


    public Select2Element openDropdown()
    {
        selectElement.find(By.cssSelector(".select2-choice")).click();
        waitUntilDropdownIsVisible();
        return this;
    }

    private void waitUntilDropdownIsVisible()
    {
        waitUntilTrue("Select2 dropdown did not appear for element " + selectElement, select2Dropdown.timed().isVisible());
    }

    public void closeDropdown()
    {
        if (select2Dropdown.isVisible())
        {
            selectElement.find(By.xpath("..")).find(By.cssSelector(".select2-choice")).click();
        }
    }

    public void chooseOption(String value)
    {
        select2Dropdown.click();
        List<PageElement> options = select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            if (option.getText().equals(value))
            {
                option.click();
                break;
            }
        }
    }

    public List<String> getAllOptions()
    {
        List<String> options = new ArrayList<String>();
        List<PageElement> select2Options = select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement select2Option : select2Options)
        {
            options.add(select2Option.getText());
        }
        return options;
    }

    public PageElement getSelectedOption()
    {
        return selectElement.find(By.xpath("..")).find(By.cssSelector(".select2-choice > .select2-chosen"));
    }
}