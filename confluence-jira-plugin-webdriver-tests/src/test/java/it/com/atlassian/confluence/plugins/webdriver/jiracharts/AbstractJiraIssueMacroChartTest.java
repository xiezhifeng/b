package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.TwoDimensionalChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.hamcrest.Matchers;
import org.junit.After;
import org.openqa.selenium.By;

import static org.junit.Assert.assertTrue;

public abstract class AbstractJiraIssueMacroChartTest extends AbstractJiraIssueMacroTest {

    protected static final String JIRA_CHART_MACRO_NAME = "jirachart";

    protected PieChartDialog dialogPieChart;
    protected JiraMacroSearchPanelDialog dialogSearchPanel;
    protected TwoDimensionalChartDialog dialogTwoDimensionalChart;
    protected CreatedVsResolvedChartDialog dialogCreatedVsResolvedChart;

    @After
    public void clear() {
        closeDialog(dialogPieChart);
        closeDialog(dialogSearchPanel);
        closeDialog(dialogTwoDimensionalChart);
    }

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication) {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editContentPage);

        // "searchForFirst" method is flaky test. It types and search too fast.
        // macroBrowserDialog.searchForFirst("jira chart").select();

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clearn field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();

        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("jira chart");
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase("jira chart"));

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        PieChartDialog dialogPieChart = pageBinder.bind(PieChartDialog.class);

        if (isAutoAuthentication) {
            if (dialogPieChart.needAuthentication()) {
                // going to authenticate
                dialogPieChart.doOAuthenticate();
            }
        }
        return dialogPieChart;
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog() {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Two Dimensional");

        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }

    protected CreatedVsResolvedChartDialog openJiraChartCreatedVsResolvedPanelDialog() {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Created vs Resolved");

        return pageBinder.bind(CreatedVsResolvedChartDialog.class);
    }

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor() {
        dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.inputJqlSearch("status = open");
        dialogCreatedVsResolvedChart.clickPreviewButton();
        assertTrue(dialogCreatedVsResolvedChart.hadChartImage());
        return dialogCreatedVsResolvedChart;
    }
}