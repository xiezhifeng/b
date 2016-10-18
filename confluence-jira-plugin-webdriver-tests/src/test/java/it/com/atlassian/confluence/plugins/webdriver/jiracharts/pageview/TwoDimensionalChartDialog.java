package it.com.atlassian.confluence.plugins.webdriver.jiracharts.pageview;


import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TwoDimensionalChartDialog extends AbstractJiraIssueMacroTest {

    @Test
    public void testTwoDimensionalChartShowMore() throws InterruptedException {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();

        dialogTwoDimensionalChart.getNumberOfResult().clear().type("1");
        dialogTwoDimensionalChart.selectYAxis("Issue Type");
        dialogTwoDimensionalChart.inputJqlSearch("KEY IN (TP-1, TP-2)");
        dialogTwoDimensionalChart.clickPreviewButton();
        assertTrue(dialogTwoDimensionalChart.isChartImageVisible());

        editPage = dialogTwoDimensionalChart.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);
        editPage.getEditor().clickSaveAndWaitForPageChange();

        pageJiraChartView = pageBinder.bind(JiraChartViewPage.class);
        assertTrue(pageJiraChartView.getChartSummary().getText(), pageJiraChartView.getChartSummary().getText().contains("Showing 1 of 2 statistics"));
        assertTrue(pageJiraChartView.getXAxis().equals("Status"));
        assertTrue(pageJiraChartView.getYAxis().equals("Issue Type"));

        assertTrue(pageJiraChartView.getShowLink().getText().contains("Show more"));
        pageJiraChartView.clickShowLink();
        assertTrue(pageJiraChartView.getChartSummary().getText().contains("Showing 2 of 2 statistics"));
        assertTrue(pageJiraChartView.getShowLink().getText().contains("Show less"));
        pageJiraChartView.clickShowLink();
        assertTrue(pageJiraChartView.getChartSummary().getText().contains("Showing 1 of 2 statistics"));
    }

}
