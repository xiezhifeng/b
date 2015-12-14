package it.com.atlassian.confluence.plugins.webdriver.jiraissues;

import java.util.List;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroPage;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class PasteJiraLinkTest extends AbstractJiraTest
{
    @BeforeClass
    public static void init() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        makeSureInEditPage();
        editPage.getEditor().getContent().clear();
    }

    @AfterClass
    public static void clean() throws Exception
    {
        cancelEditPage(editPage);
    }

    @Test
    public void testPasteJiraLinkWithIssueKey() throws Exception
    {
        String issueKey = "TOD-1";
        String jiraLink = JIRA_DISPLAY_URL + "/browse/" + issueKey;
        EditorContent content = editPage.getEditor().getContent();

        content.pasteContent(jiraLink);

        content.waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        String macroParams = macroPlaceholders.get(0).getAttribute("data-macro-parameters");
        assertThat(macroParams, containsString("key=" + issueKey));
    }

    @Test
    public void testPasteJiraLinkWithSingleIssueXML() throws Exception
    {
        String issueKey = "TOD-1";
        String jiraLink = JIRA_DISPLAY_URL + "/si/jira.issueviews:issue-xml/" + issueKey + "/" + issueKey + ".xml";
        EditorContent content = editPage.getEditor().getContent();

        content.pasteContent(jiraLink);

        content.waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        String macroParams = macroPlaceholders.get(0).getAttribute("data-macro-parameters");
        assertThat(macroParams, containsString("key=" + issueKey));
    }

    @Test
    public void testPasteJiraSprintLinkWithHaveBoardAndSprintIds() throws Exception
    {
        String boardId = "1";
        String sprintId = "2";

        String jiraSprintLink = JIRA_DISPLAY_URL + String.format("/secure/RapidBoard.jspa?rapidView=%s&sprint=%s", boardId, sprintId);
        EditorContent content = editPage.getEditor().getContent();

        content.pasteContent(jiraSprintLink);

        content.waitForInlineMacro(JIRA_SPRINT_MACRO_NAME);

        List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(JIRA_SPRINT_MACRO_NAME);
        String macroParams = macroPlaceholders.get(0).getAttribute("data-macro-parameters");
        assertThat(macroParams, containsString("boardId=" + boardId));
        assertThat(macroParams, containsString("sprintId=" + sprintId));

        // save page
        ViewPage viewPage = editPage.save();
        viewPage.doWait();
        JiraSprintMacroPage page = pageBinder.bind(JiraSprintMacroPage.class);

        String link = page.getSprintLink();
        assertThat(link, containsString("rapidViewId=" + boardId));
        assertThat(link, containsString("sprintId=" + sprintId));

        editPage = page.edit();
        editPage.doWaitUntilTinyMceIsInit();
    }

    @Test
    public void testPasteJiraSprintLinkWithHaveBoardAndSprintIdsAndOtherParams() throws Exception
    {
        String boardId = "1";
        String sprintId = "2";
        String view = "reporting";
        String chart = "sprintretrospective";
        String projectKey = "test";
        String other = "dummy";


        String jiraSprintLink = JIRA_DISPLAY_URL +
                String.format("/secure/RapidBoard.jspa?rapidView=%s&sprint=%s&view=%s&chart=%s&projectKey=%s&other=%s",
                        boardId, sprintId, view, chart, projectKey, other);

        EditorContent content = editPage.getEditor().getContent();

        content.pasteContent(jiraSprintLink);

        content.waitForInlineMacro(JIRA_SPRINT_MACRO_NAME);

        List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(JIRA_SPRINT_MACRO_NAME);
        String macroParams = macroPlaceholders.get(0).getAttribute("data-macro-parameters");

        assertThat(macroParams, containsString("boardId=" + boardId));
        assertThat(macroParams, containsString("sprintId=" + sprintId));
        assertThat(macroParams, containsString("view=" + view));
        assertThat(macroParams, containsString("chart=" + chart));
        assertThat(macroParams, containsString("projectKey=" + projectKey));
        assertThat(macroParams, not(containsString("dummy")));

        // save page
        ViewPage viewPage = editPage.save();
        viewPage.doWait();
        JiraSprintMacroPage page = pageBinder.bind(JiraSprintMacroPage.class);

        String link = page.getSprintLink();
        assertThat(link, containsString("rapidViewId=" + boardId));
        assertThat(link, containsString("sprintId=" + sprintId));
        assertThat(link, containsString("view=" + view));
        assertThat(link, containsString("chart=" + chart));
        assertThat(link, containsString("projectKey=" + projectKey));
        assertThat(link, not(containsString("dummy")));

        editPage = page.edit();
        editPage.doWaitUntilTinyMceIsInit();
    }
}
