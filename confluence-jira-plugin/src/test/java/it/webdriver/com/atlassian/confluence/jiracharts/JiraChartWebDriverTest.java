package it.webdriver.com.atlassian.confluence.jiracharts;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.PieChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;

public class JiraChartWebDriverTest extends AbstractJiraWebDriverTest
{

    public static final String JIRA_CHART_PROXY_SERVLET = "/confluence/plugins/servlet/jira-chart-proxy";

    private PieChartDialog pieChartDialog = null;

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
        if (pieChartDialog != null && pieChartDialog.isVisible())
        {
         // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            pieChartDialog.clickCancel();
            pieChartDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    private PieChartDialog openSelectJiraMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        return this.product.getPageBinder().bind(PieChartDialog.class);
    }

    private JiraIssuesDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraIssuesDialog =  this.product.getPageBinder().bind(JiraIssuesDialog.class);
        return jiraIssuesDialog;
    }

    @Test
    public void testStatType()
    {
        this.pieChartDialog = openSelectJiraMacroDialog();
        checkNotNull(this.pieChartDialog.getSelectedStatType());
    }

    @Test
    public void testJiraIssuesMacroLink()
    {
        this.pieChartDialog = openSelectJiraMacroDialog();
        checkNotNull(this.pieChartDialog.getJiraIssuesMacroAnchor());
        assertEquals(this.pieChartDialog.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
        this.jiraIssuesDialog = this.pieChartDialog.clickJiraIssuesMacroAnchor();
        assertEquals(this.jiraIssuesDialog.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    public void testDefaultChart()
    {
        this.pieChartDialog = openSelectJiraMacroDialog();
        assertEquals("Pie Chart", this.pieChartDialog.getSelectedChart());
    }

    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("project = unknow");
        pieChartDialog.clickPreviewButton();
        Assert.assertTrue("Expect to have warning JQL message inside IFrame",
                pieChartDialog.hasWarningOnIframe());
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArgs);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs);

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editContentPage.save().edit();

        pieChartDialog = openSelectJiraMacroDialog();

        Assert.assertTrue("Authentication link should be displayed", pieChartDialog.getAuthenticationLink().isVisible());
        ApplinkHelper.removeAllAppLink(client, authArgs);
    }

    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkPasteValueInJQLSearchField()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.pasteJqlSearch("TP-1");
        Poller.waitUntilTrue("key=TP-1", pieChartDialog.getPageEleJQLSearch().timed().isVisible());
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
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("status = open");
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

        EditorContent editorContent = editorPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
        editorPage.save();
    }

    private PieChartDialog insertMacroToEditor()
    {
        pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("status = open");
        pieChartDialog.clickPreviewButton();
        Assert.assertTrue(pieChartDialog.hadImageInDialog());
        return pieChartDialog;
    }

    private void checkImageInDialog(boolean hasBorder)
    {
        pieChartDialog = openAndSearch();

        if (hasBorder)
        {
            pieChartDialog.clickBorderImage();
            Assert.assertTrue(pieChartDialog.hadBorderImageInDialog());
        }
    }

    private PieChartDialog openAndSearch()
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
    
    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkInputValueInJQLSearchField()
    {
        PieChartDialog pieChartDialog = openSelectJiraMacroDialog();
        pieChartDialog.inputJqlSearch("TP-1");
        pieChartDialog.clickPreviewButton();
        Assert.assertEquals("key=TP-1", pieChartDialog.getJqlSearch());
    }

 }
