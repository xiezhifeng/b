package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class Picker extends ConfluenceAbstractPageComponent
{
    @ElementBy(cssSelector = ".jira-interation-create-issue-form")
    protected PageElement createIssueDialog;
    @ElementBy(id = "select2-drop")
    protected PageElement select2Dropdown;

    private PageElement selectElement;

    public void bindingElements(PageElement selectElement)
    {
        this.selectElement = selectElement;
    }

    public Picker openDropdown()
    {
        selectElement.find(By.xpath("..")).find(By.cssSelector(".select2-choice")).click();
        Poller.waitUntilTrue(select2Dropdown.timed().isVisible());
        return this;
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
        List<String> pickerOptions = new ArrayList<String>();
        List<PageElement> options = select2Dropdown.findAll(By.cssSelector(".select2-results > li"));
        for (PageElement option : options)
        {
            pickerOptions.add(option.getText());
        }
        return pickerOptions;
    }

    public PageElement getSelectedOption()
    {
        return selectElement.find(By.xpath("..")).find(By.cssSelector(".select2-choice > .select2-chosen"));
    }
}
