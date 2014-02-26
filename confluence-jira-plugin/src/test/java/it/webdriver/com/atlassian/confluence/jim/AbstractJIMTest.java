package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;

import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;

public class AbstractJIMTest extends AbstractJiraWebDriverTest {

	protected JiraIssuesDialog jiraIssuesDialog;

	public AbstractJIMTest() {
		super();
	}

	protected JiraIssuesDialog openJiraIssuesDialog() {
	    MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
	    macroBrowserDialog.searchForFirst("embed jira issues").select();
	    jiraIssuesDialog =  product.getPageBinder().bind(JiraIssuesDialog.class);
	    return jiraIssuesDialog;
	}

	protected JiraIssuesDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder) {
	    macroPlaceholder.click();
	    product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
	    return product.getPageBinder().bind(JiraIssuesDialog.class);
	}

	protected JiraIssuesPage createPageWithTableJiraIssueMacroAndJQL(String jql) {
	    jiraIssuesDialog = openJiraIssuesDialog();
	    jiraIssuesDialog.inputJqlSearch(jql);
	    jiraIssuesDialog.clickSearchButton();
	
	    EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
	    waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
	    editContentPage.save();
	    return bindCurrentPageToJiraIssues();
	}

	protected JiraIssuesPage createPageWithCountJiraIssueMacro(String jql) {
	    jiraIssuesDialog = openJiraIssuesDialog();
	    jiraIssuesDialog.inputJqlSearch(jql);
	    jiraIssuesDialog.clickSearchButton();
	    jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTotalCount();
	    EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
	    waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
	    editContentPage.save();
	    return bindCurrentPageToJiraIssues();
	}

	protected JiraIssuesPage gotoPage(Long pageId) {
	    product.viewPage(String.valueOf(pageId));
	    return bindCurrentPageToJiraIssues();
	}

	protected EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames,
			String jql) {
			    jiraIssuesDialog = openJiraIssuesDialog();
			    jiraIssuesDialog.inputJqlSearch(jql);
			    jiraIssuesDialog.clickSearchButton();
			    jiraIssuesDialog.openDisplayOption();
			
			    //clean all column default and add new list column
			    jiraIssuesDialog.cleanAllOptionColumn();
			    DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
			    for(String columnName : columnNames)
			    {
			        displayOptionPanel.addColumn(columnName);
			    }
			
			    EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
			    waitUntilInlineMacroAppearsInEditor(editPage, "jira");
			    EditorContent editorContent = editPage.getContent();
			    List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jira");
			    assertEquals(1, listMacroChart.size());
			
			    return editPage;
			}

    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return product.getPageBinder().bind(JiraIssuesPage.class);
    }
}