package com.atlassian.confluence.plugins.pageobjects.jirachart;


import com.atlassian.confluence.plugins.helper.JiraChartHelper;
import com.atlassian.pageobjects.elements.PageElement;

import com.google.common.base.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


public class TwoDimensionalChartDialog extends AbstractJiraChartDialog
{
    protected static final String CSS_SELECTOR_TWO_DIMENSIONAL_PANEL = "#jira-chart-content-twodimensional";


    @Override
    public PageElement getPanelBodyDialog()
    {
        return find(CSS_SELECTOR_TWO_DIMENSIONAL_PANEL);
    }

    public PageElement getXAxis()
    {
        return queryPageElement("#twodimensional-xaxis");
    }

    public PageElement getYAxis()
    {
        return queryPageElement("#twodimensional-yaxis");
    }

    public void selectYAxis(String text)
    {
        getYAxis().type(text);
    }

     public PageElement getNumberOfResult()
    {
        return queryPageElement("#twodimensional-number-of-result");
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
