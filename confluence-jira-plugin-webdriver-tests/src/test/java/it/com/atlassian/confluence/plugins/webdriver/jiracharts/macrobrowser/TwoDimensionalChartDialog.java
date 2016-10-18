package it.com.atlassian.confluence.plugins.webdriver.jiracharts.macrobrowser;


import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TwoDimensionalChartDialog extends AbstractJiraIssueMacroTest {

    @Test
    public void testSwitchToTwoDimensionalDialog() {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();
        dialogTwoDimensionalChart.openDisplayOption();
        assertNotNull(dialogTwoDimensionalChart.getNumberOfResult().isPresent());
    }

    @Test
    public void testDefaultValuesTwoDimensionalDialog() {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();

        dialogTwoDimensionalChart.openDisplayOption();

        assertEquals("5", dialogTwoDimensionalChart.getNumberOfResult().getValue());
        assertEquals("statuses", dialogTwoDimensionalChart.getXAxis().getValue());
        assertEquals("assignees", dialogTwoDimensionalChart.getYAxis().getValue());
    }

    @Test
    public void testValidateNumberOfResult() {
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
