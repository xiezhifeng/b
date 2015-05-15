package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class Select2Element extends ConfluenceAbstractPageComponent
{
    @ElementBy(id = "select2-drop", timeoutType = TimeoutType.PAGE_LOAD)
    protected PageElement select2Dropdown;

    private PageElement selectElement;

    public void bindingElements(PageElement selectElement)
    {
        this.selectElement = selectElement;
    }

    public Select2Element openDropdown()
    {
        selectElement.find(By.cssSelector(".select2-choice")).click();
        Poller.waitUntilTrue(select2Dropdown.timed().isVisible());
        return this;
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
