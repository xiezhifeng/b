package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

public class AbstractJiraChartTest extends AbstractJiraTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    protected PieChartDialog dialogPieChart;

    protected static EditContentPage editPage;
    protected static ViewPage viewPage;

    @Before
    public void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogPieChart);

        if (editPage != null && editPage.getEditor().isCancelVisibleNow()) {
            editPage.getEditor().clickCancel();
        }

        super.tearDown();
    }

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);

        // "searchForFirst" method is flaky test. It types and search too fast.
        // macroBrowserDialog.searchForFirst("jira chart").select();
        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("jira chart");
        Poller.waitUntil(macroBrowserDialog.getDialog().find(By.id("macro-browser-search")).timed().getValue(), Matchers.equalToIgnoringCase("jira chart"));
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
