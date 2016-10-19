package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

public class JiraMacroPlaceholderTest extends AbstractJiraIssueMacroSearchPanelTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1").clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        waitUntilTrue(
                editContentPage.getEditor().getContent().htmlContains("/plugins/servlet/confluence/placeholder/macro")
        );
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TSTT-1, TST-1").clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        waitUntilTrue(
                editContentPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png")
        );
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'").clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        waitUntilTrue(
                editContentPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png")
        );
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        waitUntilTrue(
                editContentPage.getEditor().getContent().htmlContains("/confluence/plugins/servlet/image-generator?totalIssues")
        );
    }
}
