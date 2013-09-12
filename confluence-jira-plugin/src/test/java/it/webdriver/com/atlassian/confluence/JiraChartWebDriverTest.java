package it.webdriver.com.atlassian.confluence;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.webdriver.utils.by.ByJquery;

public class JiraChartWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String TITLE_DIALOG_JIRA_CHART = "Insert JIRA Chart";
    private static final String LINK_HREF_MORE = "http://go.atlassian.com/confluencejiracharts";
    private static final String JIRA_CHART_PROXY_SERVLET = "/confluence/plugins/servlet/jira-chart-proxy";
    
    @Override
    public void start() throws Exception {
        super.start();
    }
    
    @Before
    public void setup() throws IOException, JSONException{
        // check to recreate applink
        setupAppLink(true);
    }
    
    private JiraChartDialog openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openMacroBrowser();
        JiraChartDialog jiraChartDialog = product.getPageBinder().bind(JiraChartDialog.class);
        jiraChartDialog.open();
        Assert.assertTrue(TITLE_DIALOG_JIRA_CHART.equals(jiraChartDialog.getTitleDialog()));
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
    
    /**
     * Test Jira Chart Macro handle invalid JQL
     */
    @Test
    public void checkInvalidJQL(){
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("project = unknow");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue("Expect to have warning JQL message inside IFrame", 
                jiraChartDialog.hasWarningOnIframe());
    }
    
//    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        removeAllAppLink();
        setupAppLink(false);
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        
        Assert.assertTrue("Authentication link should be displayed",
                jiraChartDialog.getAuthenticationLink().isVisible());
    }
    
    /**
     * check JQL search field when input value convert to JQL
     */
    @Test
    public void checkPasteValueInJQLSearchField()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
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
    
    /**
     * click button insert in Dialog
     */
    @Test
    public void clickInsertInDialog()
    {
        insertMacroToEditor();
    }
    
    /**
     * check link href more to come in left panel item of Dialog 
     */
    @Test
    public void checkMoreToComeLink()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        String hrefLink = jiraChartDialog.getLinkMoreToCome();
        Assert.assertTrue(StringUtils.isNotBlank(hrefLink) && LINK_HREF_MORE.equals(hrefLink));
    }
    
    /**
     * validate jira image in content page
     */
    @Test
    public void validateMacroInContentPage()
    {
        final EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        waitForChartDisplayOnEditor(editorPage);
        ViewPage viewPage = editorPage.save();
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
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
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
        waitForChartDisplayOnEditor(editorPage);
        
        EditorContent editorContent = editorPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
    }
    
    private void waitForChartDisplayOnEditor(final EditContentPage editorPage) {
        Poller.waitUntilTrue("Chart did not appear in edit page", 
                new TimedQuery<Boolean>(){

                    @Override
                    public long interval() {
                        return 100;
                    }

                    @Override
                    public long defaultTimeout() {
                        return 10000;
                    }

                    @Override
                    public Boolean byDefaultTimeout() {
                        return hasChartMacro();
                    }

                    @Override
                    public Boolean by(long timeoutInMillis) {
                        return hasChartMacro();
                    }

                    @Override
                    public Boolean by(long timeout, TimeUnit unit) {
                        return hasChartMacro();
                    }

                    @Override
                    public Boolean now() {
                        return hasChartMacro();
                    }
                    
                    private boolean hasChartMacro(){
                        return editorPage.getContent().getHtml().contains("data-macro-name=\"jirachart\"");
                    }
            
        });
    }
    
    private JiraChartDialog insertMacroToEditor()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hadImageInDialog());
        return jiraChartDialog;
    }
    
    private void checkImageInDialog(boolean hasBorder)
    {
        
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        if (jiraChartDialog.needAuthentication()){
            // going to authenticate
            jiraChartDialog.doOAuthenticate();
        }
        
        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hadImageInDialog());
        if(hasBorder)
        {
            jiraChartDialog.clickBorderImage();
            Assert.assertTrue(jiraChartDialog.hadBorderImageInDialog());
        }
    }
}
