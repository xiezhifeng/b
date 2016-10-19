package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

public class AbstractJiraIssueMacroSearchPanelTest extends AbstractJiraIssueMacroTest {

    private static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    protected static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");

    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser() throws Exception {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editContentPage);

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clearn field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();
        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("embed jira issues");
        Poller.waitUntil(
                searchFiled.timed().getValue(),
                Matchers.equalToIgnoringCase("embed jira issues")
        );

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelAndStartSearch(String searchValue) throws Exception {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }
}