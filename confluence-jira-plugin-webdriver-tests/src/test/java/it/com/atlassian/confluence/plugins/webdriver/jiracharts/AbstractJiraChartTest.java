package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import org.junit.After;
import org.junit.Assert;

public abstract class AbstractJiraChartTest extends AbstractJiraIssueMacroTest {

    @After
    public void tearDown() throws Exception {
        closeDialog(dialogPieChart);
        closeDialog(dialogCreatedVsResolvedChart);
        closeDialog(dialogTwoDimensionalChart);
        closeDialog(dialogSearchPanel);
    }

    protected void checkImageInDialog(boolean hasBorder) {
        dialogPieChart = openPieChartAndSearch();
        if (hasBorder) {
            dialogPieChart.openDisplayOption();
            dialogPieChart.clickBorderImage();
            Assert.assertTrue(dialogPieChart.hadBorderImageInDialog());
        }
    }

 }
