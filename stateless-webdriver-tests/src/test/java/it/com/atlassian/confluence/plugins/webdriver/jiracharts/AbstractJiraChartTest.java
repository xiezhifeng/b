package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.TwoDimensionalChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

import static org.junit.Assert.assertTrue;

public class AbstractJiraChartTest extends AbstractJiraTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    protected static EditContentPage editPage;
    protected static ViewPage viewPage;
    protected CreatedVsResolvedChartDialog dialogCreatedVsResolvedChart = null;
    protected TwoDimensionalChartDialog dialogTwoDimensionalChart;
    protected PieChartDialog dialogPieChart;
    protected JiraMacroSearchPanelDialog dialogSearchPanel;

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

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);

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

        if (isAutoAuthentication)
        {
            if (dialogPieChart.needAuthentication())
            {
                // going to authenticate
                dialogPieChart.doOAuthenticate();
            }
        }

        return dialogPieChart;
    }

    protected PieChartDialog openPieChartAndSearch()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.clickPreviewButton();

        Assert.assertTrue(dialogPieChart.hadImageInDialog());
        return dialogPieChart;
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

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor()
    {
        dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.inputJqlSearch("status = open");
        dialogCreatedVsResolvedChart.clickPreviewButton();
        assertTrue(dialogCreatedVsResolvedChart.hadChartImage());
        return dialogCreatedVsResolvedChart;
    }

    protected CreatedVsResolvedChartDialog openJiraChartCreatedVsResolvedPanelDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Created vs Resolved");

        return pageBinder.bind(CreatedVsResolvedChartDialog.class);
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Two Dimensional");

        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }
 }
