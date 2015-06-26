package it.com.atlassian.confluence.plugins.webdriver.jiracharts;


import com.atlassian.pageobjects.elements.query.Poller;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CreatedVsResolvedChartDialogWithoutSavingTest extends AbstractJiraChartWithoutSavingTest
{
    @Test
    public void testSwitchToCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        assertNotNull(dialogCreatedVsResolvedChart.getSelectedForPeriodName().text());
    }

    @Test
    public void testDefaultValuesCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();

        assertEquals("daily", dialogCreatedVsResolvedChart.getSelectedForPeriodName().value());
        assertEquals("30", dialogCreatedVsResolvedChart.getDaysPrevious());
        assertTrue(StringUtils.isBlank(dialogCreatedVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void testHourlyPeriodNamePreviousCreatedVsResolvedChart()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Hourly");
        dialogCreatedVsResolvedChart.setDaysPrevious("30");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());

    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Daily");
        dialogCreatedVsResolvedChart.setDaysPrevious("500");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Weekly");
        dialogCreatedVsResolvedChart.setDaysPrevious("1751");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testMonthlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Monthly");
        dialogCreatedVsResolvedChart.setDaysPrevious("7501");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolved()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Quarterly");
        dialogCreatedVsResolvedChart.setDaysPrevious("22500");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Quarterly");
        dialogCreatedVsResolvedChart.setDaysPrevious("22501");
        assertTrue(!dialogCreatedVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testYearlyPeriodNameCreatedVsResolvedHasError()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.setSelectedForPeriodName("Yearly");
        dialogCreatedVsResolvedChart.setDaysPrevious("36501");
        assertTrue(StringUtils.isNotBlank(dialogCreatedVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void checkInputValueCreatedVsResolvedChartInJQLSearchField()
    {
        this.dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();

        dialogCreatedVsResolvedChart.inputJqlSearch("TP-1");
        dialogCreatedVsResolvedChart.clickPreviewButton();

        Poller.waitUntil(dialogCreatedVsResolvedChart.getJQLSearchElement().timed().getValue(), Matchers.equalToIgnoringCase("key=TP-1"));
    }

    @Test
    public void checkBorderCreatedVsResolvedChart()
    {
        dialogCreatedVsResolvedChart = openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor();
        dialogCreatedVsResolvedChart.openDisplayOption();
        dialogCreatedVsResolvedChart.clickBorderImage();
        assertTrue(dialogCreatedVsResolvedChart.hadBorderImageInDialog());
    }

}
