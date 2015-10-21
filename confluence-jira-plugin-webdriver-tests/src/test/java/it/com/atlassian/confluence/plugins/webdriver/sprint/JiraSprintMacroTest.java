package it.com.atlassian.confluence.plugins.webdriver.sprint;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroPage;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static it.com.atlassian.confluence.plugins.webdriver.model.SprintStatus.*;
import static org.junit.Assert.assertTrue;

public class JiraSprintMacroTest extends AbstractJiraSprintMacroTest
{
    @Test
    public void testBoardsAndSprintsLoadedCorrectly()
    {
        List<String> boards = sprintDialog.getAllBoardOptions();

        // it should include only 2 scrum boards and no kanban board
        assertEquals("Boards are not correctly loaded", 2, boards.size());

        sprintDialog.selectBoard(SCRUM_BOARD_1.getName());
        List<String> sprints = sprintDialog.getAllSprintOptions();

        assertEquals("Sprints are not correctly loaded", SCRUM_BOARD_1.getSprints().size(), sprints.size());
    }

    @Test
    public void testInsertSprintMacroSuccessEditMode()
    {
        sprintDialog.selectBoard(SCRUM_BOARD_1.getName());
        sprintDialog.selectSprint(SPRINT2.getName());

        sprintDialog.insert();

        // check edit mode
        EditorContent editorContent = editPage.getEditor().getContent();
        editorContent.waitForInlineMacro(JIRA_SPRINT_MACRO_NAME);

        List<MacroPlaceholder> macros = editorContent.macroPlaceholderFor(JIRA_SPRINT_MACRO_NAME);
        assertEquals("incorrect number of inserted macros", 1, macros.size());

        MacroPlaceholder sprintMacro = macros.get(0);
        String[] params = sprintMacro.getAttribute("data-macro-parameters").split("\\|");

        assertEquals("incorrect board id", "boardId=1", params[0]);
        assertEquals("incorrect sprint id", "sprintId=2", params[2]);

        // click again to check the dialog display correctly
        sprintDialog = openSprintDialogFromMacroPlaceholder(editorContent, sprintMacro);

        String selectedBoard = sprintDialog.getSelectedBoard();
        String selectedSprint = sprintDialog.getSelectedSprint();

        assertEquals("Board is not displayed correctly", SCRUM_BOARD_1.getName(), selectedBoard);
        assertEquals("Sprint is not displayed correctly", SPRINT2.getName(), selectedSprint);
    }

    @Test
    public void testInsertSprintMacroSuccessViewMode()
    {
        JiraSprintMacroPage sprintPage = createSprintPage(SCRUM_BOARD_1, SPRINT2);

        assertEquals("Sprint name is not stored correctly", SPRINT2.getName(), sprintPage.getSprintName());
        assertEquals("Sprint status is not displayed correctly", ACTIVE.name(), sprintPage.getSprintStatus());

        // return edit mode for other tests
        editPage = gotoEditTestPage(user.get());
    }

    @Test
    public void testClosedSprintGoToSprintReport()
    {
        JiraSprintMacroPage sprintPage = createSprintPage(SCRUM_BOARD_1, SPRINT1);

        assertEquals("Sprint name is not stored correctly", SPRINT1.getName(), sprintPage.getSprintName());
        assertEquals("Sprint status is not displayed correctly", CLOSED.name(), sprintPage.getSprintStatus());
        assertTrue("Sprint is not linked to sprint report", sprintPage.getSprintLink().contains("GHLocateSprintOnBoard.jspa?rapidViewId=1&sprintId=1"));

        // return to edit mode for other tests
        editPage = gotoEditTestPage(user.get());
    }
}
