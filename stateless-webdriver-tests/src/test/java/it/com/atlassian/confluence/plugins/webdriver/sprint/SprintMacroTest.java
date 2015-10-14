package it.com.atlassian.confluence.plugins.webdriver.sprint;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SprintMacroTest extends AbstractSprintTest
{
    @Test
    public void testBoardsAndSprintsLoadedCorrectly()
    {
        List<String> boards = sprintDialog.getAllBoardOptions();
        // remove default board select option
        boards.remove(0);

        // it should include only 2 scrum boards and no kanban board
        assertEquals("Boards are not correctly loaded", 2, boards.size());

        sprintDialog.selectBoard(SRUM_BOARD_1.getName());
        List<String> sprints = sprintDialog.getAllSprintOptions();
        // remove first empty sprint option
        sprints.remove(0);

        assertEquals("Sprints are not correctly loaded", SRUM_BOARD_1.getSprints().size(), sprints.size());
    }

    @Test
    public void testInsertSprintMacroSuccessEditMode() throws Exception
    {
        sprintDialog.selectBoard(SRUM_BOARD_1.getName());
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

        String selectedBoard = sprintDialog.getSeletedBoard();
        String selectedSprint = sprintDialog.getSelectedSprint();

        assertEquals("Board is not displayed correctly", SRUM_BOARD_1.getName(), selectedBoard);
        assertEquals("Sprint is not displayed correctly", SPRINT2.getName(), selectedSprint);
    }

    @Test
    public void testInsertSprintMacroSuccessViewMode() throws Exception
    {
        sprintDialog.selectBoard(SRUM_BOARD_1.getName());
        sprintDialog.selectSprint(SPRINT2.getName());

        sprintDialog.insert();

        // TODO: check view mode

        // TODO: return edit status for other tests
    }
}
