package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import java.util.List;

import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraIssueFilterDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraWebDriverTest;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

public class JiraChartTest extends AbstractJiraWebDriverTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    protected PieChartDialog pieChartDialog = null;
    protected JiraIssueFilterDialog jiraIssueFilterDialog;

    protected static EditContentPage editPage;
    protected static ViewPage viewPage;

    @BeforeClass
    public static void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void prepare() throws Exception
    {
        // before each tests, make sure we are standing on edit page.
        if (viewPage != null && viewPage.canEdit())
        {
            editPage = viewPage.edit();
            Poller.waitUntilTrue("Edit page is ready", editPage.getEditor().isEditorCurrentlyActive());
            editPage.getEditor().getContent().clear();
        }
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(pieChartDialog);
        closeDialog(jiraIssueFilterDialog);
        super.tearDown();
    }

    protected PieChartDialog openSelectJiraMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);
        macroBrowserDialog.searchForFirst("jira chart").select();

        return product.getPageBinder().bind(PieChartDialog.class);
    }

    protected PieChartDialog insertMacroToEditor()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("status = open");
        pieChartDialog.clickPreviewButton();
        Assert.assertTrue(pieChartDialog.hadImageInDialog());
        return pieChartDialog;
    }

    protected void checkImageInDialog(boolean hasBorder)
    {
        pieChartDialog = openAndSearch();

        if (hasBorder)
        {
            pieChartDialog.openDisplayOption();
            pieChartDialog.clickBorderImage();
            Assert.assertTrue(pieChartDialog.hadBorderImageInDialog());
        }
    }

    protected PieChartDialog openAndSearch()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        if (pieChartDialog.needAuthentication())
        {
            // going to authenticate
            pieChartDialog.doOAuthenticate();
        }

        pieChartDialog.inputJqlSearch("status = open");
        pieChartDialog.clickPreviewButton();
        Assert.assertTrue(pieChartDialog.hadImageInDialog());
        return pieChartDialog;
    }

    @Test
    public void testStatType()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.openDisplayOption();
        checkNotNull(pieChartDialog.getSelectedStatType());
    }

    @Test
    public void testJiraIssuesMacroLink()
    {
        pieChartDialog = openSelectJiraMacroDialog();

        checkNotNull(pieChartDialog.getJiraIssuesMacroAnchor());
        assertEquals(pieChartDialog.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");

        jiraIssueFilterDialog = pieChartDialog.clickJiraIssuesMacroAnchor();
        assertEquals(jiraIssueFilterDialog.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    public void testDefaultChart()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        assertEquals("Pie Chart", pieChartDialog.getSelectedChart());
    }

    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch(" = unknown");
        pieChartDialog.clickPreviewButton();

        Assert.assertTrue("Expect to have warning JQL message inside IFrame",
                pieChartDialog.hasWarningOnIframe());
    }

//    @Test
//    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
//    {
//        ApplinkHelper.removeAllAppLink(client, authArgs);
//        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs);
//
//        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
//        // this now since the setUp() method already places us in the editor context
//        editPage.save().edit();
//
//        pieChartDialog = openSelectJiraMacroDialog();
//
//        Assert.assertTrue("Authentication link should be displayed", pieChartDialog.getAuthenticationLink().isVisible());
//        ApplinkHelper.removeAllAppLink(client, authArgs);
//    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkPasteValueInJQLSearchField()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.pasteJqlSearch("TP-1");

        waitUntilTrue("key=TP-1", pieChartDialog.getPageEleJQLSearch().isVisible());
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
        pieChartDialog = openAndSearch();
        pieChartDialog.openDisplayOption();
        pieChartDialog.clickShowInforCheckbox();
        pieChartDialog.hasInfoBelowImage();
    }

    /**
     * click button insert in Dialog
     */
    @Test
    public void clickInsertInDialog()
    {
        pieChartDialog = insertMacroToEditor();
    }

    /**
     * validate jira image in content page
     */
    @Test
    public void validateMacroInContentPage()
    {
        insertMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, "jirachart");

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
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("status = open");
        pieChartDialog.openDisplayOption();
        pieChartDialog.setValueWidthColumn("400.0");
        pieChartDialog.clickPreviewButton();
        Assert.assertTrue(pieChartDialog.hasWarningValWidth());
    }

    /**
     * validate jira chart macro in RTE
     */
    @Test
    public void validateMacroInEditor()
    {
        final EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editorPage, "jirachart");

        EditorContent editorContent = editorPage.getEditor().getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getTimedHtml().byDefaultTimeout();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
        editorPage.save();
    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkInputValueInJQLSearchField()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("TP-1");
        pieChartDialog.clickPreviewButton();
        Assert.assertEquals("key=TP-1", pieChartDialog.getJqlSearch());
    }

 }
