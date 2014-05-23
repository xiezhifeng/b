package it.webdriver.com.atlassian.confluence.pageobjects.jirachart;


import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.base.Function;
import it.webdriver.com.atlassian.confluence.helper.JiraChartHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class TwoDimensionalChartDialog extends JiraChartDialog
{
    @ElementBy(cssSelector = "#jira-chart-content-twodimensional #jira-chart-search-input")
    private SelectElement jqlSearch;

    @ElementBy(id = "twodimensional-number-of-result")
    private PageElement numberOfResult;

    @ElementBy(className = "twodimensional-number-of-result-error")
    private PageElement numberOfResultError;

    @ElementBy(id = "twodimensional-xaxis")
    private SelectElement xAxis;

    @ElementBy(id = "twodimensional-yaxis")
    private SelectElement yAxis;

    public TwoDimensionalChartDialog()
    {
        super("jira-chart");

    }

    public TwoDimensionalChartDialog inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }

    public PageElement getNumberOfResult()
    {
        return numberOfResult;
    }

    public PageElement getNumberOfResultError()
    {
        return numberOfResultError;
    }

    public String getJqlSearch()
    {
        return jqlSearch.getValue();
    }

    public void clickPreviewButton()
    {
        driver.findElement(By.cssSelector("#jira-chart-content-twodimensional #jira-chart-search-button")).click();
    }

    public boolean isTwoDimensionalChartTableDisplay() {
        return getTwoDimensionalChartTable(new Function<WebElement, Boolean>()
        {
            @Override
            public Boolean apply(WebElement element)
            {
                return element.isDisplayed();
            }
        });
    }

    public void selectXAxis(String text)
    {
        Poller.waitUntilTrue(xAxis.timed().isEnabled());
        xAxis.type(text);
    }

    public void selectYAxis(String text)
    {
        Poller.waitUntilTrue(yAxis.timed().isEnabled());
        yAxis.type(text);
    }

    private  <R> R getTwoDimensionalChartTable(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.className("two-dimensional-chart"), checker, driver);
    }
}
