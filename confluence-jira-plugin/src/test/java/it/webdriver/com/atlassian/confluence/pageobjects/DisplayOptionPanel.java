package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import junit.framework.Assert;
import org.openqa.selenium.By;

import java.util.List;

public class DisplayOptionPanel
{
    @ElementBy(cssSelector = "#jiraMacroDlg > .jql-display-opts-inner")
    private PageElement displayOptionsPanel;

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

    public boolean isIssueTypeRadioEnable(String value)
    {
        PageElement element = getRadioBtn(value);
        return element.isEnabled();
    }
}
