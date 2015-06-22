package it.webdriver.com.atlassian.confluence.jiracharts;


import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.JiraChartViewPage;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.PieChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.TwoDimensionalChartDialog;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore
public class JiraTwoDimensionalChartDialogWebDriverTest extends AbstractJiraWebDriverTest
{
    private TwoDimensionalChartDialog twoDimensionalChartDialog;

    @Before
    public void start() throws Exception
    {
        super.start();
    }

    @After
    public void tearDown() throws Exception
    {
        if (twoDimensionalChartDialog != null && twoDimensionalChartDialog.isVisible())
        {
            twoDimensionalChartDialog.clickCancel();
            twoDimensionalChartDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    @Test
    public void testSwitchToTwoDimensionalDialog()
    {
        this.twoDimensionalChartDialog = openTwoDimensionalChartDialog();

        assertNotNull(twoDimensionalChartDialog.getNumberOfResult().isPresent());
    }

    @Test
    public void testDefaultValuesTwoDimensionalDialog()
    {
        this.twoDimensionalChartDialog = openTwoDimensionalChartDialog();
        assertEquals("5", twoDimensionalChartDialog.getNumberOfResult().getValue());
        assertEquals("statuses", twoDimensionalChartDialog.getxAxis().getValue());
        assertEquals("assignees", twoDimensionalChartDialog.getyAxis().getValue());
    }

    @Test
    public void testValidateNumberOfResult()
    {
        this.twoDimensionalChartDialog = openTwoDimensionalChartDialog();
        twoDimensionalChartDialog.getNumberOfResult().clear().type("text");
        twoDimensionalChartDialog.inputJqlSearch("status=open");
        assertFalse(twoDimensionalChartDialog.getNumberOfResultError().getText().isEmpty());

        twoDimensionalChartDialog.getNumberOfResult().clear().type("10");
        twoDimensionalChartDialog.inputJqlSearch("status=open");
        assertTrue(twoDimensionalChartDialog.getNumberOfResultError().getText().isEmpty());

        twoDimensionalChartDialog.getNumberOfResult().clear().type("0");
        twoDimensionalChartDialog.inputJqlSearch("status=open");
        assertFalse(twoDimensionalChartDialog.getNumberOfResultError().getText().isEmpty());
    }

    @Test
    public void testTwoDimensionalChartData()
    {
        this.twoDimensionalChartDialog = openTwoDimensionalChartDialog();
        twoDimensionalChartDialog.inputJqlSearch("project=TP");
        twoDimensionalChartDialog.clickPreviewButton();
        assertTrue(twoDimensionalChartDialog.isTwoDimensionalChartTableDisplay());

        EditContentPage editContentPage = twoDimensionalChartDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        JiraChartViewPage page = product.getPageBinder().bind(JiraChartViewPage.class);
        assertTrue(page.getChartSummary().getText().contains("Showing 1 of 1 statistics"));
        assertTrue(page.getXAxis().equals("Status"));
        assertTrue(page.getYAxis().equals("Assignee"));
    }

    @Test
    public void testTwoDimensionalChartShowMore() throws InterruptedException
    {
        this.twoDimensionalChartDialog = openTwoDimensionalChartDialog();
        twoDimensionalChartDialog.getNumberOfResult().clear().type("1");
        twoDimensionalChartDialog.selectYAxis("Issue Type");
        twoDimensionalChartDialog.inputJqlSearch("KEY IN (TP-1, TP-2)");
        twoDimensionalChartDialog.clickPreviewButton();
        assertTrue(twoDimensionalChartDialog.isTwoDimensionalChartTableDisplay());

        EditContentPage editContentPage = twoDimensionalChartDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        JiraChartViewPage page = product.getPageBinder().bind(JiraChartViewPage.class);
        assertTrue(page.getChartSummary().getText(), page.getChartSummary().getText().contains("Showing 1 of 2 statistics"));
        assertTrue(page.getXAxis().equals("Status"));
        assertTrue(page.getYAxis().equals("Issue Type"));

        assertTrue(page.getShowLink().getText().contains("Show more"));
        page.clickShowLink();
        assertTrue(page.getChartSummary().getText().contains("Showing 2 of 2 statistics"));
        assertTrue(page.getShowLink().getText().contains("Show less"));
        page.clickShowLink();
        assertTrue(page.getChartSummary().getText().contains("Showing 1 of 2 statistics"));
    }

    private TwoDimensionalChartDialog openTwoDimensionalChartDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        PieChartDialog pieChartDialog =  this.product.getPageBinder().bind(PieChartDialog.class);
        return  (TwoDimensionalChartDialog) pieChartDialog.selectChartDialog
                (TwoDimensionalChartDialog.class, pieChartDialog.getJiraTwoDimensionalChart(), "Two Dimensional");
    }

}
