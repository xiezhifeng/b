package it.com.atlassian.confluence.plugins.webdriver.jiracharts.pageview;


import it.com.atlassian.confluence.plugins.webdriver.jiracharts.AbstractJiraIssueMacroChartTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TwoDimensionalChartDialogViewTest extends AbstractJiraIssueMacroChartTest
{
    @Test
    public void testTwoDimensionalChartShowMore() throws InterruptedException
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();

        dialogTwoDimensionalChart.getNumberOfResult().clear().type("1");
        dialogTwoDimensionalChart.selectYAxis("Issue Type");
        dialogTwoDimensionalChart.inputJqlSearch("KEY IN (TP-1, TP-2)");
        dialogTwoDimensionalChart.clickPreviewButton();
        assertTrue(dialogTwoDimensionalChart.isChartImageVisible());

        editContentPage = dialogTwoDimensionalChart.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        JiraChartViewPage pageJiraChartView = pageBinder.bind(JiraChartViewPage.class);
        assertThat(pageJiraChartView.getChartSummary().getText(), containsString("Showing 1 of 2 statistics"));
        assertTrue(pageJiraChartView.getXAxis().equals("Status"));
        assertTrue(pageJiraChartView.getYAxis().equals("Issue Type"));

        assertThat(pageJiraChartView.getShowLink().getText(), containsString("Show more"));

        pageJiraChartView.clickShowLink();
        assertThat(pageJiraChartView.getChartSummary().getText(), containsString("Showing 2 of 2 statistics"));
        assertThat(pageJiraChartView.getShowLink().getText(), containsString("Show less"));

        pageJiraChartView.clickShowLink();
        assertThat(pageJiraChartView.getChartSummary().getText(), containsString("Showing 1 of 2 statistics"));
    }

}
