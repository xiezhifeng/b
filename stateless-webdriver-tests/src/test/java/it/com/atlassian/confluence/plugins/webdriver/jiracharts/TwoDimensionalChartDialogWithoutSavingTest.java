package it.com.atlassian.confluence.plugins.webdriver.jiracharts;


import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TwoDimensionalChartDialogWithoutSavingTest extends AbstractJiraChartWithoutSavingTest
{
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
}
