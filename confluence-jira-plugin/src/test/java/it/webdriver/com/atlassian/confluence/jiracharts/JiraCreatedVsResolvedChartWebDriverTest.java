package it.webdriver.com.atlassian.confluence.jiracharts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.CreatedVsResolvedChart;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.utils.by.ByJquery;

public class JiraCreatedVsResolvedChartWebDriverTest extends AbstractJiraWebDriverTest 
{
    private CreatedVsResolvedChart createdVsResolvedChart = null;
    @Before
    public void setupJiraChartTestData() throws Exception
    {
        // Check to recreate applink if necessary
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, authArgs);
    }

    @After
    public void tearDown() throws Exception
    {
        if (createdVsResolvedChart != null && createdVsResolvedChart.isVisible())
        {
         // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            createdVsResolvedChart.clickCancel();
            createdVsResolvedChart.waitUntilHidden();
        }
        super.tearDown();
    }

    @Test
    public void testSwitchToCreatedVsResolvedChart()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();

        assertNotNull(createdVsResolvedChart.getSelectedForPeriodName().text());
    }

    @Test
    public void testDefaultValuesCreatedVsResolvedChart()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        
        assertEquals("daily", createdVsResolvedChart.getSelectedForPeriodName().value());
        assertEquals("30", createdVsResolvedChart.getDaysPrevious());
        assertTrue(StringUtils.isBlank(createdVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void testHourlyPeriodNamePreviousCreatedVsResolvedChart()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Hourly");
        createdVsResolvedChart.setDaysPrevious("30");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());

    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Daily");
        createdVsResolvedChart.setDaysPrevious("500");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Weekly");
        createdVsResolvedChart.setDaysPrevious("1751");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testMonthlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Monthly");
        createdVsResolvedChart.setDaysPrevious("7501");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolved()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Quarterly");
        createdVsResolvedChart.setDaysPrevious("22500");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Quarterly");
        createdVsResolvedChart.setDaysPrevious("22501");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testYearlyPeriodNameCreatedVsResolvedHasError()
    {
        this.createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.setSelectedForPeriodName("Yearly");
        createdVsResolvedChart.setDaysPrevious("36501");
        assertTrue(StringUtils.isNotBlank(createdVsResolvedChart.getDaysPreviousError()));
    }

    @Test
    public void checkInputValueCreatedVsResolvedChartInJQLSearchField()
    {
        CreatedVsResolvedChart createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        
        createdVsResolvedChart.inputJqlSearch("TP-1");
        createdVsResolvedChart.clickPreviewButton();
        Assert.assertEquals("key=TP-1", createdVsResolvedChart.getJqlSearch());
    }

    @Test
    public void checkBorderCreatedVsResolvedChart()
    {
        createdVsResolvedChart = insertCreatedVsResolvedChartMacroToEditor();
        createdVsResolvedChart.clickBorderImage();
        assertTrue(createdVsResolvedChart.hadBorderImageInDialog());
    }

    @Test
    public void validateCreatedVsResolvedMacroInContentPage()
    {
        insertCreatedVsResolvedChartMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        ViewPage viewPage = editContentPage.save();
        PageElement pageElement = viewPage.getMainContent();
        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JiraChartWebDriverTest.JIRA_CHART_PROXY_SERVLET));
    }

    private CreatedVsResolvedChart insertCreatedVsResolvedChartMacroToEditor()
    {
        createdVsResolvedChart = openSelectJiraChartCreatedVsResolvedMacroDialog();
        createdVsResolvedChart.inputJqlSearch("status = open");
        createdVsResolvedChart.clickPreviewButton();
        Assert.assertTrue(createdVsResolvedChart.hadImageInDialog());
        return createdVsResolvedChart;
    }
    
    private CreatedVsResolvedChart openSelectJiraChartCreatedVsResolvedMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        JiraChartDialog jiraChartDialog =  this.product.getPageBinder().bind(JiraChartDialog.class);
        CreatedVsResolvedChart createdResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        return createdResolvedChart;
    }

}
