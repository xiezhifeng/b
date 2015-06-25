package it.com.atlassian.confluence.plugins.webdriver.jiracharts;


import com.atlassian.confluence.plugins.pageobjects.jirachart.JiraChartViewPage;
import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jirachart.TwoDimensionalChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TwoDimensionalChartDialogTest extends AbstractJiraChartTest
{
    protected TwoDimensionalChartDialog dialogTwoDimensionalChart;
    protected static JiraChartViewPage pageJiraChartView;

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogTwoDimensionalChart);
        super.tearDown();
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Two Dimensional");

        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }

    @Test
    public void testSwitchToTwoDimensionalDialog()
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();
        assertNotNull(dialogTwoDimensionalChart.getNumberOfResult().isPresent());
    }

    @Test
    public void testDefaultValuesTwoDimensionalDialog()
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();

        assertEquals("5", dialogTwoDimensionalChart.getNumberOfResult().getValue());
        assertEquals("statuses", dialogTwoDimensionalChart.getxAxis().getValue());
        assertEquals("assignees", dialogTwoDimensionalChart.getyAxis().getValue());
    }

    @Test
    public void testValidateNumberOfResult()
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();
        dialogTwoDimensionalChart.getNumberOfResult()
                .clear().type("text")
                .javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        Poller.waitUntil(dialogTwoDimensionalChart.getNumberOfResultError().timed().getText(), Matchers.not(Matchers.isEmptyString()));

        dialogTwoDimensionalChart.getNumberOfResult()
                .clear().type("10")
                .javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        Poller.waitUntil(dialogTwoDimensionalChart.getNumberOfResultError().timed().getText(), Matchers.isEmptyString());

        dialogTwoDimensionalChart.getNumberOfResult()
                .clear().type("0")
                .javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        Poller.waitUntil(dialogTwoDimensionalChart.getNumberOfResultError().timed().getText(), Matchers.not(Matchers.isEmptyString()));
    }

    @Test
    public void testTwoDimensionalChartData()
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();
        dialogTwoDimensionalChart.inputJqlSearch("project=TP");
        dialogTwoDimensionalChart.clickPreviewButton();
        assertTrue(dialogTwoDimensionalChart.isTwoDimensionalChartTableDisplay());

        EditContentPage editContentPage = dialogTwoDimensionalChart.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        pageJiraChartView = pageBinder.bind(JiraChartViewPage.class);
        assertTrue(pageJiraChartView.getChartSummary().getText().contains("Showing 1 of 1 statistics"));
        assertTrue(pageJiraChartView.getXAxis().equals("Status"));
        assertTrue(pageJiraChartView.getYAxis().equals("Assignee"));
    }

    @Test
    public void testTwoDimensionalChartShowMore() throws InterruptedException
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();

        dialogTwoDimensionalChart.getNumberOfResult().clear().type("1");
        dialogTwoDimensionalChart.selectYAxis("Issue Type");
        dialogTwoDimensionalChart.inputJqlSearch("KEY IN (TP-1, TP-2)");
        dialogTwoDimensionalChart.clickPreviewButton();
        assertTrue(dialogTwoDimensionalChart.isTwoDimensionalChartTableDisplay());

        editPage = dialogTwoDimensionalChart.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, "jirachart");
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
