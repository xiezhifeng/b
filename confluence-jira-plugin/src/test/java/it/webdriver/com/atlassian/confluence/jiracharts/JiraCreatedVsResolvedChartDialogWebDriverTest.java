package it.webdriver.com.atlassian.confluence.jiracharts;


import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.utils.by.ByJquery;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.PieChartDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JiraCreatedVsResolvedChartDialogWebDriverTest extends AbstractJiraWebDriverTest
{
    private CreatedVsResolvedChartDialog createdVsResolvedChartDialog = null;

    @Before
    public void start() throws Exception
    {
        super.start();
    }

    @After
    public void tearDown() throws Exception
    {
        if (createdVsResolvedChartDialog != null && createdVsResolvedChartDialog.isVisible())
        {
         // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            createdVsResolvedChartDialog.clickCancel();
            createdVsResolvedChartDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    @Test
    public void testSwitchToCreatedVsResolvedChart()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();

        assertNotNull(createdVsResolvedChartDialog.getSelectedForPeriodName().text());
    }

    @Test
    public void testDefaultValuesCreatedVsResolvedChart()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        
        assertEquals("daily", createdVsResolvedChartDialog.getSelectedForPeriodName().value());
        assertEquals("30", createdVsResolvedChartDialog.getDaysPrevious());
        assertTrue(StringUtils.isBlank(createdVsResolvedChartDialog.getDaysPreviousError()));
    }

    @Test
    public void testHourlyPeriodNamePreviousCreatedVsResolvedChart()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Hourly");
        createdVsResolvedChartDialog.setDaysPrevious("30");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());

    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Daily");
        createdVsResolvedChartDialog.setDaysPrevious("500");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Weekly");
        createdVsResolvedChartDialog.setDaysPrevious("1751");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testMonthlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Monthly");
        createdVsResolvedChartDialog.setDaysPrevious("7501");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolved()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Quarterly");
        createdVsResolvedChartDialog.setDaysPrevious("22500");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Quarterly");
        createdVsResolvedChartDialog.setDaysPrevious("22501");
        assertTrue(!createdVsResolvedChartDialog.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testYearlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.setSelectedForPeriodName("Yearly");
        createdVsResolvedChartDialog.setDaysPrevious("36501");
        assertTrue(StringUtils.isNotBlank(createdVsResolvedChartDialog.getDaysPreviousError()));
    }

    @Test
    public void checkInputValueCreatedVsResolvedChartInJQLSearchField()
    {
        CreatedVsResolvedChartDialog createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        
        createdVsResolvedChartDialog.inputJqlSearch("TP-1");
        createdVsResolvedChartDialog.clickPreviewButton();
        Assert.assertEquals("key=TP-1", createdVsResolvedChartDialog.getJqlSearch());
    }

    @Test
    public void checkBorderCreatedVsResolvedChart()
    {
        createdVsResolvedChartDialog = insertCreatedVsResolvedChartMacroToEditor();
        createdVsResolvedChartDialog.clickBorderImage();
        assertTrue(createdVsResolvedChartDialog.hadBorderImageInDialog());
    }

    @Test
    public void validateCreatedVsResolvedMacroInContentPage()
    {
        insertCreatedVsResolvedChartMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        ViewPage viewPage = editContentPage.save();
        PageElement pageElement = viewPage.getMainContent();
        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JiraChartWebDriverTest.JIRA_CHART_BASE_64_PREFIX));
    }

    private CreatedVsResolvedChartDialog insertCreatedVsResolvedChartMacroToEditor()
    {
        createdVsResolvedChartDialog = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChartDialog.inputJqlSearch("status = open");
        createdVsResolvedChartDialog.clickPreviewButton();
        Assert.assertTrue(createdVsResolvedChartDialog.hadImageInDialog());
        return createdVsResolvedChartDialog;
    }
    
    private CreatedVsResolvedChartDialog openSelectJiraChartCreatedVsResolvedMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        PieChartDialog pieChartDialog =  this.product.getPageBinder().bind(PieChartDialog.class);
        return  (CreatedVsResolvedChartDialog) pieChartDialog.selectChartDialog
                (CreatedVsResolvedChartDialog.class, pieChartDialog.getJiraCreatedVsResolvedChart(), "Created vs Resolved");
    }

}
