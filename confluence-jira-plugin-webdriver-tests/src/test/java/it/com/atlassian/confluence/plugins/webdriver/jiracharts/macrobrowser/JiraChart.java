package it.com.atlassian.confluence.plugins.webdriver.jiracharts.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

public class JiraChart extends AbstractJiraIssueMacroTest {

    @Test
    public void testStatType() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.openDisplayOption();
        checkNotNull(dialogPieChart.getSelectedStatType());
    }

    @Test
    public void testJiraIssuesMacroLink() {
        dialogPieChart = openPieChartDialog(true);

        checkNotNull(dialogPieChart.getJiraIssuesMacroAnchor());
        assertEquals(dialogPieChart.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");

        dialogSearchPanel = dialogPieChart.clickJiraIssuesMacroAnchor();
        assertEquals(dialogSearchPanel.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    public void testDefaultChart() {
        dialogPieChart = openPieChartDialog(true);
        assertEquals("Pie Chart", dialogPieChart.getSelectedChart());
    }

    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL() {
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
    public void checkPasteValueInJQLSearchField() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.pasteJqlSearch("TP-1");

        waitUntilTrue("key=TP-1", dialogPieChart.getJqlSearchElement().timed().isVisible());
    }

    /**
     * check draw image in dialog when click editorPreview button
     */
    @Test
    public void checkImageInDialog() {
        checkImageInDialog(false);
    }

    /**
     * check border image when click check box border.
     */
    @Test
    public void checkBorderImageInDialog() {
        checkImageInDialog(true);
    }

    @Test
    public void checkShowInfoInDialog() {
        dialogPieChart = openPieChartAndSearch();
        dialogPieChart.openDisplayOption();
        dialogPieChart.clickShowInforCheckbox();
        dialogPieChart.hasInfoBelowImage();
    }

    /**
     * show warning if input wrong format value Width column
     */
    @Test
    public void checkFormatWidthInDialog() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.openDisplayOption();
        dialogPieChart.setValueWidthColumn("400.0");
        dialogPieChart.clickPreviewButton();
        Assert.assertTrue(dialogPieChart.hasWarningValWidth());
    }


    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkInputValueInJQLSearchField() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("TP-1");
        dialogPieChart.clickPreviewButton();
        Poller.waitUntil(
                dialogPieChart.getJqlSearchElement().timed().getValue(),
                Matchers.equalToIgnoringCase("key=TP-1")
        );
    }

    /**
     * validate jira chart macro in RTE
     */
    @Test
    public void validateMacroInEditor() {
        openPieChartAndSearch().clickInsertDialog();
        EditorContent editorContent = editPage.getEditor().getContent();
        editorContent.waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());

        Poller.waitUntilTrue(editorContent.htmlContains("data-macro-name=\"jirachart\""));
    }
}
