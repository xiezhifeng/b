package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
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

public class AbstractJiraChartTest extends AbstractJiraTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    protected PieChartDialog dialogPieChart;

    protected static EditContentPage editPage;
    protected static ViewPage viewPage;

    @BeforeClass
    public static void init() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        if (editPage != null && !editPage.getEditor().isCancelVisibleNow())
        {
            editPage = gotoEditTestPage(user.get());
        }
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogPieChart);
        super.tearDown();
    }

    @AfterClass
    public static void clean() throws Exception
    {
        if (editPage != null && editPage.getEditor().isCancelVisibleNow())
        {
            editPage.getEditor().clickCancel();
        }
    }

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);

        // "searchForFirst" method is flaky test. It types and search too fast.
        // macroBrowserDialog.searchForFirst("jira chart").select();
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();

        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("jira chart");
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase("jira chart"));
        macroItems.iterator().next().select();

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
 }
