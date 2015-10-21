package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;


import com.atlassian.pageobjects.elements.PageElement;

import org.openqa.selenium.By;

public class CreatedVsResolvedChartDialog extends AbstractJiraChartDialog
{
    public static final String CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED = "#jira-chart-content-createdvsresolved";

    @Override
    public PageElement getPanelBodyDialog()
    {
        return find(CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED);
    }

    public PageElement getSelectedForPeriodElement()
    {
        return queryPageElement("#created-vs-resolved-chart-periodName");
    }

    public PageElement getDaysPreviousElement()
    {
        return queryPageElement("#created-vs-resolved-chart-daysprevious");
    }

    public void setDaysPrevious(String value)
    {
        PageElement daysPreviousElement = getDaysPreviousElement();
        daysPreviousElement.type(value);
        triggerChangeEvent(daysPreviousElement);
    }

    public String getDaysPreviousError()
    {
        return getPanelBodyDialog().find(By.cssSelector(".days-previous-error")).getText();
    }


    public void clickBorderImage()
    {
        queryPageElement("#jira-createdvsresolved-chart-show-border").click();
    }
}
