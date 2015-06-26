package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import java.util.List;

import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

public class JiraChartTest extends AbstractJiraChartTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    protected PieChartDialog dialogPieChart;
    protected JiraMacroSearchPanelDialog dialogSearchPanel;

    @After
    public void tearDown() throws Exception
    {
        closeDialog(dialogPieChart);
        closeDialog(dialogSearchPanel);
        super.tearDown();
    }

    @Test
    public void testStatType()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.openDisplayOption();
        checkNotNull(dialogPieChart.getSelectedStatType());
    }

    @Test
    public void testJiraIssuesMacroLink()
    {
        dialogPieChart = openPieChartDialog(true);

        checkNotNull(dialogPieChart.getJiraIssuesMacroAnchor());
        assertEquals(dialogPieChart.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");

        dialogSearchPanel = dialogPieChart.clickJiraIssuesMacroAnchor();
        assertEquals(dialogSearchPanel.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    @Ignore("change to qunit test - not necessary to be WD test")
    public void testDefaultChart()
    {
        dialogPieChart = openPieChartDialog(true);
        assertEquals("Pie Chart", dialogPieChart.getSelectedChart());
    }

    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch(" = unknown");
        dialogPieChart.clickPreviewButton();

        Assert.assertTrue("Expect to have warning JQL message inside IFrame",
                dialogPieChart.hasWarningOnIframe());
    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkPasteValueInJQLSearchField()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.pasteJqlSearch("TP-1");

        waitUntilTrue("key=TP-1", dialogPieChart.getPageEleJQLSearch().isVisible());
    }

    /**
     * check draw image in dialog when click editorPreview button
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
        dialogPieChart = openPieChartAndSearch();
        dialogPieChart.openDisplayOption();
        dialogPieChart.clickShowInforCheckbox();
        dialogPieChart.hasInfoBelowImage();
    }

    /**
     * validate jira image in content page
     */
    @Test
    public void validateMacroInContentPage()
    {
        openPieChartAndSearch().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(By.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }

    /**
     * show warning if input wrong format value Width column
     */
    @Test
    public void checkFormatWidthInDialog()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.openDisplayOption();
        dialogPieChart.setValueWidthColumn("400.0");
        dialogPieChart.clickPreviewButton();
        Assert.assertTrue(dialogPieChart.hasWarningValWidth());
    }

    /**
     * validate jira chart macro in RTE
     */
    @Test
    public void validateMacroInEditor()
    {
        final EditContentPage editorPage = openPieChartAndSearch().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        EditorContent editorContent = editorPage.getEditor().getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());

        Poller.waitUntilTrue( editorContent.htmlContains("data-macro-name=\"jirachart\""));
        editorPage.save();
    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkInputValueInJQLSearchField()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("TP-1");
        dialogPieChart.clickPreviewButton();
        Poller.waitUntil(dialogPieChart.getJQLSearchElement().timed().getValue(), Matchers.equalToIgnoringCase("key=TP-1"));
    }

 }