package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;

import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.JiraChartHelper;
import com.atlassian.pageobjects.elements.PageElement;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.function.Function;


public class TwoDimensionalChartDialog extends AbstractJiraChartDialog
{
    private static final String CSS_SELECTOR_TWO_DIMENSIONAL_PANEL = "#jira-chart-content-twodimensional";

    @Override
    public PageElement getPanelBodyDialog()
    {
        PageElement panelBodyDialog = find(CSS_SELECTOR_TWO_DIMENSIONAL_PANEL);
        Poller.waitUntilTrue(panelBodyDialog.timed().isVisible());
        return panelBodyDialog;
    }

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
        return getPanelBodyDialog().find(By.cssSelector(".twodimensional-number-of-result-error"));
    }

    @Override
    public  <R> R getChartImage(Function<WebElement, R> checker)
    {
         return JiraChartHelper.getElementOnFrame(By.className("two-dimensional-chart"), checker, driver);
    }
}
