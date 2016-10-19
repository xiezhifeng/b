package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraMacroPropertyPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class AbstractJiraIssueMacroSearchPanelTest extends AbstractJiraIssueMacroTest {

    private static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    protected static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    protected static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";

    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelAndStartSearch(String searchValue) throws Exception {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }

    protected MacroPlaceholder createMacroPlaceholderFromQueryString(String jiraIssuesMacro, String macroName) {
        EditorContent content = editContentPage.getEditor().getContent();
        content.type(jiraIssuesMacro);
        content.waitForInlineMacro(macroName);
        final List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(macroName);
        assertThat("No macro placeholder found", macroPlaceholders, hasSize(greaterThanOrEqualTo(1)));
        return macroPlaceholders.iterator().next();
    }

    protected JiraMacroSearchPanelDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder) {
        editContentPage.getEditor().getContent().doubleClickEditInlineMacro(macroPlaceholder.getAttribute("data-macro-name"));
        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }

    protected JiraMacroPropertyPanel getJiraMacroPropertyPanel(MacroPlaceholder macroPlaceholder) {
        macroPlaceholder.click();
        return pageBinder.bind(JiraMacroPropertyPanel.class);
    }
}