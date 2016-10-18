package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractJiraChartTest extends AbstractJiraTest
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
        if (editPage == null)
        {
            editPage = gotoEditTestPage(user.get());
        }
        else
        {
            if (editPage.getEditor().isCancelVisibleNow())
            {
                // in editor page.
                editPage.getEditor().getContent().clear();
            }
            else
            {
                // in view page, and then need to go to edit page.
                editPage = gotoEditTestPage(user.get());
            }
        }
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