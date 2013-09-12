package it.webdriver.com.atlassian.confluence;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;

public class JiraChartWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String TITLE_DIALOG_JIRA_CHART = "Insert JIRA Chart";
    private static final String LINK_HREF_MORE = "http://go.atlassian.com/confluencejiracharts";
    private static final String JIRA_CHART_PROXY_SERVLET = "/confluence/plugins/servlet/jira-chart-proxy";
    
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
        EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        ViewPage viewPage = editorPage.save();
        PageElement pageElement = viewPage.getMainContent();
        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content span img")).getAttribute("src");
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
        EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        EditorContent editorContent = editorPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
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
