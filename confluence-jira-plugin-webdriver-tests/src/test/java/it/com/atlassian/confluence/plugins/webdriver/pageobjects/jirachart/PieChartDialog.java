package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;

import com.atlassian.pageobjects.elements.PageElement;


public class PieChartDialog extends AbstractJiraChartDialog
{
    protected static final String CSS_SELECTOR_PIE_CHART = "#jira-chart-content-pie";


    @Override
    public PageElement getPanelBodyDialog()
    {
        return find(CSS_SELECTOR_PIE_CHART);
    }

    public void setValueWidthColumn(String val)
    {
        queryPageElement("#jira-chart-width").clear().type(val);
    }

    public void clickBorderImage()
    {
        queryPageElement("#jira-pie-chart-show-border").click();
    }

    public void clickShowInforCheckbox()
    {
        queryPageElement("#jira-pie-chart-show-infor").click();
    }

    public String getSelectedStatType()
    {
        return queryPageElement("#jira-chart-statType").getValue();
    }
}
