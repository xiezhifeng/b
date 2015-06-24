package it.com.atlassian.confluence.plugins.webdriver.jiracharts;


import com.atlassian.confluence.plugins.pageobjects.jirachart.JiraChartViewPage;
import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jirachart.TwoDimensionalChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        PieChartDialog pieChartDialog = openPieChartDialog();

        return  (TwoDimensionalChartDialog) pieChartDialog.selectChartDialog
                (TwoDimensionalChartDialog.class, pieChartDialog.getJiraTwoDimensionalChart(), "Two Dimensional");
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
        dialogTwoDimensionalChart.getNumberOfResult().clear().type("text").type(Keys.TAB);
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        assertFalse(dialogTwoDimensionalChart.getNumberOfResultError().getText().isEmpty());

        dialogTwoDimensionalChart.getNumberOfResult().clear().type("10").type(Keys.TAB);
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        assertTrue(dialogTwoDimensionalChart.getNumberOfResultError().getText().isEmpty());

        dialogTwoDimensionalChart.getNumberOfResult().clear().type("0").type(Keys.TAB);
        dialogTwoDimensionalChart.inputJqlSearch("status=open");
        assertFalse(dialogTwoDimensionalChart.getNumberOfResultError().getText().isEmpty());
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

        pageJiraChartView = product.getPageBinder().bind(JiraChartViewPage.class);
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

        pageJiraChartView = product.getPageBinder().bind(JiraChartViewPage.class);
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
