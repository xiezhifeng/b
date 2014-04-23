package it.webdriver.com.atlassian.confluence.jiracharts;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.CreatedVsResolvedChart;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class JiraChartWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String LINK_HREF_MORE = "http://go.atlassian.com/confluencejiracharts";
    
    public static final String JIRA_CHART_PROXY_SERVLET = "/confluence/plugins/servlet/jira-chart-proxy";

    private JiraChartDialog jiraChartDialog = null;

    private JiraIssuesDialog jiraIssuesDialog;

    @Before
    public void setupJiraChartTestData() throws Exception
    {
        // Check to recreate applink if necessary
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, authArgs);
    }

    @After
    public void tearDown() throws Exception
    {
        if (jiraChartDialog != null && jiraChartDialog.isVisible())
        {
         // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            jiraChartDialog.clickCancel();
            jiraChartDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    private JiraChartDialog openSelectMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        return this.product.getPageBinder().bind(JiraChartDialog.class);
    }

    private JiraIssuesDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraIssuesDialog =  this.product.getPageBinder().bind(JiraIssuesDialog.class);
        return jiraIssuesDialog;
    }

    @Test
    public void testJiraIssuesMacroLink()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        checkNotNull(this.jiraChartDialog.getJiraIssuesMacroAnchor());
        assertEquals(this.jiraChartDialog.getJiraIssuesMacroAnchor().getAttribute("class"), "jira-left-panel-link");
        this.jiraIssuesDialog = this.jiraChartDialog.clickJiraIssuesMacroAnchor();
        assertEquals(this.jiraIssuesDialog.getJiraChartMacroAnchor().getAttribute("class"), "jira-left-panel-link");
    }

    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL()
    {
        jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("project = unknow");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue("Expect to have warning JQL message inside IFrame",
                jiraChartDialog.hasWarningOnIframe());
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArgs);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs);

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editContentPage.save().edit();

        jiraChartDialog = openSelectMacroDialog();

        Assert.assertTrue("Authentication link should be displayed",jiraChartDialog.getAuthenticationLink().isVisible());
        ApplinkHelper.removeAllAppLink(client, authArgs);
    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkPasteValueInJQLSearchField()
    {
        jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.pasteJqlSearch("TP-1");
        Poller.waitUntilTrue("key=TP-1", jiraChartDialog.getPageEleJQLSearch().timed().isVisible());
    }

    /**
     * check draw image in dialog when click preview button
     */
    @Test
    public void checkImageInDialog()
    {
        checkImageInDialog(false);
    }

    /**
     * check border image when click check box border.
     */
    @Test
    public void checkBorderImageInDialog()
    {
        checkImageInDialog(true);
    }

    @Test
    public void checkShowInfoInDialog()
    {
        jiraChartDialog = openAndSearch();
        jiraChartDialog.clickShowInforCheckbox();
        jiraChartDialog.hasInfoBelowImage();
    }

    /**
     * click button insert in Dialog
     */
    @Test
    public void clickInsertInDialog()
    {
        jiraChartDialog = insertMacroToEditor();
    }

    /**
     * check link href more to come in left panel item of Dialog
     */
    @Test
    public void checkMoreToComeLink()
    {
        jiraChartDialog = openSelectMacroDialog();
        String hrefLink = jiraChartDialog.getLinkMoreToCome();
        Assert.assertTrue(StringUtils.isNotBlank(hrefLink) && LINK_HREF_MORE.equals(hrefLink));
    }

    /**
     * validate jira image in content page
     */
    @Test
    public void validateMacroInContentPage()
    {
        insertMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jirachart");
        ViewPage viewPage = editContentPage.save();
        PageElement pageElement = viewPage.getMainContent();
        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_PROXY_SERVLET));
    }

    /**
     * show warning if input wrong format value Width column
     */
    @Test
    public void checkFormatWidthInDialog()
    {
        jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.setValueWidthColumn("400.0");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hasWarningValWidth());
    }

    /**
     * validate jira chart macro in RTE
     */
    @Test
    public void validateMacroInEditor()
    {
        final EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editorPage, "jirachart");

        EditorContent editorContent = editorPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
        editorPage.save();
    }

    private JiraChartDialog insertMacroToEditor()
    {
        jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hadImageInDialog());
        return jiraChartDialog;
    }

    private void checkImageInDialog(boolean hasBorder)
    {
        jiraChartDialog = openAndSearch();

        if (hasBorder)
        {
            jiraChartDialog.clickBorderImage();
            Assert.assertTrue(jiraChartDialog.hadBorderImageInDialog());
        }
    }

    private JiraChartDialog openAndSearch()
    {
        jiraChartDialog = openSelectMacroDialog();
        if (jiraChartDialog.needAuthentication())
        {
            // going to authenticate
            jiraChartDialog.doOAuthenticate();
        }

        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hadImageInDialog());
        return jiraChartDialog;
    }
    
    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkInputValueInJQLSearchField()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("TP-1");
        jiraChartDialog.clickPreviewButton();
        Assert.assertEquals("key=TP-1", jiraChartDialog.getJqlSearch());
    }

    @Test
    public void testSwitchToCreatedVsResolvedChart()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        assertNotNull(createdVsResolvedChart.getSelectedForPeriodName().text());
    }

    @Test
    public void testDefaultValuesCreatedVsResolvedChart()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        assertEquals("daily", createdVsResolvedChart.getSelectedForPeriodName().value());
        assertEquals("30", createdVsResolvedChart.getDaysPrevious());
        assertTrue(createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testHourlyPeriodNamePreviousCreatedVsResolvedChart()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option hourly = new Option() {
            @Override
            public String id() {
                return "hourly";
            }

            @Override
            public String value() {
                return "hourly";
            }

            @Override
            public String text() {
                return "Hourly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(hourly);
        createdVsResolvedChart.setDaysPrevious("30");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());

    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChart()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart =  jiraChartDialog.clickOnCreatedVsResolved();
        Option daily = new Option() {
            @Override
            public String id() {
                return "daily";
            }

            @Override
            public String value() {
                return "daily";
            }

            @Override
            public String text() {
                return "Daily";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(daily);
        createdVsResolvedChart.setDaysPrevious("30");
        assertTrue(createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testDailyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option daily = new Option() {
            @Override
            public String id() {
                return "daily";
            }

            @Override
            public String value() {
                return "daily";
            }

            @Override
            public String text() {
                return "Daily";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(daily);
        createdVsResolvedChart.setDaysPrevious("500");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChart()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option weekly = new Option() {
            @Override
            public String id() {
                return "weekly";
            }

            @Override
            public String value() {
                return "weekly";
            }

            @Override
            public String text() {
                return "Weekly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(weekly);
        createdVsResolvedChart.setDaysPrevious("1750");
        assertTrue(createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testWeeklyPeriodNameCreatedVsResolvedChartHasError()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option weekly = new Option() {
            @Override
            public String id() {
                return "weekly";
            }

            @Override
            public String value() {
                return "weekly";
            }

            @Override
            public String text() {
                return "Weekly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(weekly);
        createdVsResolvedChart.setDaysPrevious("1751");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }


    @Test
    public void testMonthlyPeriodNameCreatedVsResolved()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option monthly = new Option() {
            @Override
            public String id() {
                return "monthly";
            }

            @Override
            public String value() {
                return "monthly";
            }

            @Override
            public String text() {
                return "Monthly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(monthly);
        createdVsResolvedChart.setDaysPrevious("7500");
        assertTrue(createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testMonthlyPeriodNameCreatedVsResolvedHasError()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option monthly = new Option() {
            @Override
            public String id() {
                return "monthly";
            }

            @Override
            public String value() {
                return "monthly";
            }

            @Override
            public String text() {
                return "Monthly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(monthly);
        createdVsResolvedChart.setDaysPrevious("7501");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolved()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option quarterly = new Option() {
            @Override
            public String id() {
                return "quarterly";
            }

            @Override
            public String value() {
                return "quarterly";
            }

            @Override
            public String text() {
                return "Quarterly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(quarterly);
        createdVsResolvedChart.setDaysPrevious("22500");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void testQuarterlyPeriodNameCreatedVsResolvedHasError()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option quarterly = new Option() {
            @Override
            public String id() {
                return "quarterly";
            }

            @Override
            public String value() {
                return "quarterly";
            }

            @Override
            public String text() {
                return "Quarterly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(quarterly);
        createdVsResolvedChart.setDaysPrevious("22501");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }


    @Test
    public void testYearlyPeriodNameCreatedVsResolved()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option yearly = new Option() {
            @Override
            public String id() {
                return "yearly";
            }

            @Override
            public String value() {
                return "yearly";
            }

            @Override
            public String text() {
                return "Yearly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(yearly);
        createdVsResolvedChart.setDaysPrevious("36500");
        assertTrue(createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }


    @Test
    public void testYearlyPeriodNameCreatedVsResolvedHasError()
    {
        this.jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        Option yearly = new Option() {
            @Override
            public String id() {
                return "yearly";
            }

            @Override
            public String value() {
                return "yearly";
            }

            @Override
            public String text() {
                return "Yearly";
            }
        };

        createdVsResolvedChart.setSelectedForPeriodName(yearly);
        createdVsResolvedChart.setDaysPrevious("36501");
        assertTrue(!createdVsResolvedChart.getDaysPreviousError().isEmpty());
    }

    @Test
    public void checkInputValueCreatedVsResolvedChartInJQLSearchField()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.clickOnCreatedVsResolved();
        jiraChartDialog.inputJqlSearch("TP-1");
        jiraChartDialog.clickPreviewButton();
        Assert.assertEquals("key=TP-1", jiraChartDialog.getJqlSearch());
    }

    @Test
    public void checkBorderCreatedVsResolvedChart()
    {
        CreatedVsResolvedChart createdVsResolvedChart = openAndSearchCreatedVsResolvedChart();
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
        Assert.assertTrue(srcImg.contains(JIRA_CHART_PROXY_SERVLET));
    }

    private CreatedVsResolvedChart openAndSearchCreatedVsResolvedChart()
    {
        jiraChartDialog = openSelectMacroDialog();
        CreatedVsResolvedChart createdVsResolvedChart = jiraChartDialog.clickOnCreatedVsResolved();
        if (createdVsResolvedChart.needAuthentication())
        {
            // going to authenticate
            createdVsResolvedChart.doOAuthenticate();
        }

        createdVsResolvedChart.inputJqlSearch("status = open");
        createdVsResolvedChart.clickPreviewButton();
        Assert.assertTrue(createdVsResolvedChart.hadImageInDialog());
        return createdVsResolvedChart;
    }

    private CreatedVsResolvedChart insertCreatedVsResolvedChartMacroToEditor()
    {
        CreatedVsResolvedChart createdVsResolvedChart = openAndSearchCreatedVsResolvedChart();
        createdVsResolvedChart.inputJqlSearch("status = open");
        createdVsResolvedChart.clickPreviewButton();
        Assert.assertTrue(createdVsResolvedChart.hadImageInDialog());
        return createdVsResolvedChart;
    }

}
