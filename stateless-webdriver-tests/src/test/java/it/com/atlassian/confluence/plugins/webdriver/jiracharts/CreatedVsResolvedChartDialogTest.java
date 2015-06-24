package it.com.atlassian.confluence.plugins.webdriver.jiracharts;



import com.atlassian.confluence.plugins.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.utils.by.ByJquery;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CreatedVsResolvedChartDialogTest extends AbstractJiraChartTest
{
    protected CreatedVsResolvedChartDialog dialogCreatedVsResolvedChart = null;

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogCreatedVsResolvedChart);
        super.tearDown();
    }

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor()
    {
        dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.inputJqlSearch("status = open");
        dialogCreatedVsResolvedChart.clickPreviewButton();
        assertTrue(dialogCreatedVsResolvedChart.hadImageInDialog());
        return dialogCreatedVsResolvedChart;
    }

    protected CreatedVsResolvedChartDialog openAndSelectJiraChartCreatedVsResolvedDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog();
        Dialog dialog = pieChartDialog.selectChartDialog(CreatedVsResolvedChartDialog.class, pieChartDialog.getJiraCreatedVsResolvedChart(), "Created vs Resolved");

        return  (CreatedVsResolvedChartDialog) dialog;
    }


    @Test
    public void testSwitchToCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        assertNotNull(dialogCreatedVsResolvedChart.getSelectedForPeriodName().text());
    }

    @Test
    public void testDefaultValuesCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();

        assertEquals("daily", dialogCreatedVsResolvedChart.getSelectedForPeriodName().value());
        assertEquals("30", dialogCreatedVsResolvedChart.getDaysPrevious());
        assertTrue(StringUtils.isBlank(dialogCreatedVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void testHourlyPeriodNamePreviousCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Hourly");
        dialogCreatedVsResolvedChart.setDaysPrevious("30");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());

    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Daily");
        dialogCreatedVsResolvedChart.setDaysPrevious("500");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Weekly");
        dialogCreatedVsResolvedChart.setDaysPrevious("1751");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testMonthlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Monthly");
        dialogCreatedVsResolvedChart.setDaysPrevious("7501");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolved()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Quarterly");
        dialogCreatedVsResolvedChart.setDaysPrevious("22500");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Quarterly");
        dialogCreatedVsResolvedChart.setDaysPrevious("22501");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testYearlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Yearly");
        dialogCreatedVsResolvedChart.setDaysPrevious("36501");
        assertTrue(StringUtils.isNotBlank(dialogCreatedVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void checkInputValueCreatedVsResolvedChartInJQLSearchField()
    {
        this.dialogCreatedVsResolvedChart = openAndSelectJiraChartCreatedVsResolvedDialog();

        dialogCreatedVsResolvedChart.inputJqlSearch("TP-1");
        dialogCreatedVsResolvedChart.clickPreviewButton();

        Assert.assertEquals("key=TP-1", dialogCreatedVsResolvedChart.getJqlSearch());
    }

    @Test
    public void checkBorderCreatedVsResolvedChart()
    {
        dialogCreatedVsResolvedChart = openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.clickBorderImage();
        assertTrue(dialogCreatedVsResolvedChart.hadBorderImageInDialog());
    }


    @Test
    public void validateCreatedVsResolvedMacroInContentPage()
    {
        openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, "jirachart");

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }

}
