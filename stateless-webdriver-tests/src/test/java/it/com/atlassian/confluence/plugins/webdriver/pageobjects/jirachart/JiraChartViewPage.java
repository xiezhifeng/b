package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;

import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;

public class JiraChartViewPage extends ViewPage
{

    @ElementBy(className = "two-dimensional-chart-table")
    private PageElement twoDimensionalChartTable;

    @ElementBy(className = "chart-summary")
    private PageElement chartSummary;

    @ElementBy(cssSelector = ".show-link-container a")
    private PageElement showLink;
    public PageElement getChartSummary()
    {
        return chartSummary;
    }

    public String getYAxis()
    {
        return twoDimensionalChartTable.find(ByJquery.$("tbody tr th:first-child")).getText();
    }

    public String getXAxis()
    {
        return twoDimensionalChartTable.find(ByJquery.$("tbody tr th::nth-child(2)")).getText();
    }

    public PageElement getShowLink()
    {
        return showLink;
    }

    public void clickShowLink()
    {
        String text = showLink.getText();
        showLink.click();
        if (text.contains("Show more")) {
            Poller.waitUntilTrue(showLink.timed().hasText("Show less"));
        }
        else
        {
            Poller.waitUntilTrue(showLink.timed().hasText("Show more"));
        }
    }
}
