package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;


public class PieChartDialog extends AbstractJiraChartDialog
{
    public void setValueWidthColumn(String val)
    {
        queryPageElement("#jira-chart-width").clear().type(val);
    }

    public void clickBorderImage()
    {
        queryPageElement("#jira-pie-chart-show-border").click();
    }

    public void clickShowInfoCheckbox()
    {
        queryPageElement("#jira-pie-chart-show-infor").click();
    }

    public String getSelectedStatType()
    {
        return queryPageElement("#jira-chart-statType").getValue();
    }
}
