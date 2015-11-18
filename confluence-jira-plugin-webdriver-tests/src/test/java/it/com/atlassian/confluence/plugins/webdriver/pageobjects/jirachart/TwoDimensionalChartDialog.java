package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;


import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.JiraChartHelper;
import com.atlassian.pageobjects.elements.PageElement;

import com.google.common.base.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


public class TwoDimensionalChartDialog extends AbstractJiraChartDialog
{
    public PageElement getXAxis()
    {
        PageElement xAxis = queryPageElement("#twodimensional-xaxis");
        Poller.waitUntilTrue(xAxis.timed().isVisible());
        return xAxis;
    }

    public PageElement getYAxis()
    {
        PageElement yAxis = queryPageElement("#twodimensional-yaxis");
        Poller.waitUntilTrue(yAxis.timed().isVisible());
        return yAxis;
    }

    public void selectYAxis(String text)
    {
        getYAxis().type(text);
    }

    public PageElement getNumberOfResult()
    {
        PageElement numberOfResult = queryPageElement("#twodimensional-number-of-result");
        Poller.waitUntilTrue(numberOfResult.timed().isVisible());
        return numberOfResult;
    }

    public PageElement getNumberOfResultError()
    {
        return getCurrentTabPanel().find(By.cssSelector(".twodimensional-number-of-result-error"));
    }

    @Override
    public  <R> R getChartImage(Function<WebElement, R> checker)
    {
         return JiraChartHelper.getElementOnFrame(By.className("two-dimensional-chart"), checker, driver);
    }
}
