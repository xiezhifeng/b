package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

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

    public Select2Element(PageElement selectElement)
    {
        this.selectElement = selectElement;
    }

    public Select2Element openDropdown()
    {
        PageElement pageElement = selectElement.find(By.cssSelector(".select2-choice"));
        Poller.waitUntilTrue(pageElement.timed().isVisible());

        pageElement.javascript().execute("jQuery(arguments[0]).trigger('mousedown')");
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
        Poller.waitUntilTrue(select2Dropdown.find(By.cssSelector(".select2-results")).timed().isVisible());
        Poller.waitUntilFalse(select2Dropdown.find(By.className("select2-searching")).timed().isVisible());

        List<PageElement> options = getAllOptionsPageElement();

        for (PageElement option : options)
        {
            if (option.getText().equals(value))
            {
                option.select();
                break;
            }
        }

        Poller.waitUntilEquals(value, selectElement.find(By.className("select2-chosen")).timed().getText());
    }

    public List<String> getAllOptions()
    {
        List<PageElement> select2Options = getAllOptionsPageElement();
        List<String> options = new ArrayList<String>();

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

    public List<PageElement> getAllOptionsPageElement()
    {
        Poller.waitUntilTrue(select2Dropdown.find(By.cssSelector(".select2-results")).timed().isVisible());
        Poller.waitUntilFalse(select2Dropdown.find(By.className("select2-searching")).timed().isVisible());

        return select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
    }
}