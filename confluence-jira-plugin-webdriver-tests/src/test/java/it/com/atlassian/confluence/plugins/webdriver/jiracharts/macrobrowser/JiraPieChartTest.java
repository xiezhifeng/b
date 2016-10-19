package it.com.atlassian.confluence.plugins.webdriver.jiracharts.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import it.com.atlassian.confluence.plugins.webdriver.jiracharts.AbstractJiraIssueMacroChartTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JiraPieChartTest extends AbstractJiraIssueMacroChartTest {

    private JiraMacroSearchPanelDialog dialogSearchPanel;

    @After
    public void clearSearchDialog() {
        closeDialog(dialogSearchPanel);
    }

    @Test
    public void testStatType() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.openDisplayOption();
        assertNotNull(dialogPieChart.getSelectedStatType());
    }

    @Test
    public void testJiraIssuesMacroLink() {
        dialogPieChart = openPieChartDialog(true);

        assertNotNull(dialogPieChart.getJiraIssuesMacroAnchor());
        assertEquals(
                dialogPieChart.getJiraIssuesMacroAnchor().getAttribute("class"),
                "item-button jira-left-panel-link"
        );

        dialogSearchPanel = dialogPieChart.clickJiraIssuesMacroAnchor();
        assertEquals(
                dialogSearchPanel.getJiraChartMacroAnchor().getAttribute("class"),
                "item-button jira-left-panel-link"
        );
    }

    @Test
    public void testDefaultChart() {
        dialogPieChart = openPieChartDialog(true);
        assertEquals("Pie Chart", dialogPieChart.getSelectedChart());
    }

    @Test
    public void checkInvalidJQL() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch(" = unknown");
        dialogPieChart.clickPreviewButton();

        assertTrue(
                "Expect to have warning JQL message inside IFrame",
                dialogPieChart.hasWarningOnIframe()
        );
    }

    @Test
    public void checkPasteValueInJQLSearchField() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.pasteJqlSearch("TP-1");
        waitUntilTrue("key=TP-1", dialogPieChart.getJqlSearchElement().timed().isVisible());
    }

    @Test
    public void checkImageInDialog() {
        checkImageInDialog(false);
    }

    @Test
    public void checkBorderImageInDialog() {
        checkImageInDialog(true);
    }

    @Test
    public void checkFormatWidthInDialog() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.openDisplayOption();
        dialogPieChart.setValueWidthColumn("400.0");
        dialogPieChart.clickPreviewButton();
        assertTrue(dialogPieChart.hasWarningValWidth());
    }

    @Test
    public void checkInputValueInJQLSearchField() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("TP-1");
        dialogPieChart.clickPreviewButton();
        waitUntil(
                dialogPieChart.getJqlSearchElement().timed().getValue(),
                equalToIgnoringCase("key=TP-1")
        );
    }

    @Test
    public void validateMacroInEditor() {
        openPieChartAndSearch().clickInsertDialog();
        EditorContent editorContent = editContentPage.getEditor().getContent();
        editorContent.waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        assertEquals(1, listMacroChart.size());
        waitUntilTrue(editorContent.htmlContains("data-macro-name=\"jirachart\""));
    }

    private void checkImageInDialog(boolean hasBorder) {
        dialogPieChart = openPieChartAndSearch();
        if (hasBorder) {
            dialogPieChart.openDisplayOption();
            dialogPieChart.clickBorderImage();

            assertTrue(dialogPieChart.hadBorderImageInDialog());
        }
    }

    private PieChartDialog openPieChartAndSearch() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.clickPreviewButton();
        assertTrue(dialogPieChart.hadImageInDialog());
        return dialogPieChart;
    }
}