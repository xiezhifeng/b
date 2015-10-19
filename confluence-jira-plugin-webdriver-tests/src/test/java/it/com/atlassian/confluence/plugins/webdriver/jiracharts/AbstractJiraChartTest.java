package it.com.atlassian.confluence.plugins.webdriver.jiracharts;


import org.junit.After;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

import static org.junit.Assert.assertTrue;

public class AbstractJiraChartTest extends AbstractJiraTest
{
    @BeforeClass
    public static void init() throws Exception
    {
        AbstractJiraTest.start();
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        getReadyOnEditTestPage();
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogPieChart);
        closeDialog(dialogCreatedVsResolvedChart);
        closeDialog(dialogTwoDimensionalChart);
        closeDialog(dialogSearchPanel);
    }

    @AfterClass
    public static void cleanUp() throws Exception
    {
        cancelEditPage(editPage);
    }

    protected void checkImageInDialog(boolean hasBorder)
    {
        dialogPieChart = openPieChartAndSearch();

        if (hasBorder)
        {
            dialogPieChart.openDisplayOption();
            dialogPieChart.clickBorderImage();

            Assert.assertTrue(dialogPieChart.hadBorderImageInDialog());
        }
    }
 }
