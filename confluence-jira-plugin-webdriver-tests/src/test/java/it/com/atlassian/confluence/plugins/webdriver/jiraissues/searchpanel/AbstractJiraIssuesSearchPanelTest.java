package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.google.common.collect.ImmutableList;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class AbstractJiraIssuesSearchPanelTest extends AbstractJiraTest
{
    protected static final List<String> LIST_TEST_COLUMN = ImmutableList.of("Issue Type", "Resolved", "Summary", "Key");
    protected static final List<String> LIST_MULTIVALUE_COLUMN = ImmutableList.of("Summary", "Issue Type", "Key", "Component/s", "Fix Version/s");
    protected static final List<String> LIST_URL_TEST_COLUMN = ImmutableList.of("Summary", "Issue Type", "URL", "Key");
    protected static List<String> LIST_DEFAULT_COLUMN = ImmutableList.of("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    @BeforeClass
    public static void init() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        if (editPage == null)
        {
            editPage = gotoEditTestPage(user.get());
        }
        else
        {
            if (editPage.getEditor().isCancelVisibleNow())
            {
                // in editor page.
                editPage.getEditor().getContent().clear();
            }
            else
            {
                // in view page, and then need to go to edit page.
                editPage = gotoEditTestPage(user.get());
            }
        }
    }

    @After
    public void closeJiraSearchDialog() throws Exception
    {
        closeDialog(jiraMacroSearchPanelDialog);
    }

    @AfterClass
    public static void clean() throws Exception
    {
        cancelEditPage(editPage);
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelAndStartSearch(String searchValue) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }


    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return pageBinder.bind(JiraIssuesPage.class);
    }

}
