package it.com.atlassian.confluence.plugins.webdriver.sprint;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SprintMacroTest extends AbstractSprintTest
{
    @Test
    public void testInsertSprintMacroSuccess() throws Exception
    {
        sprintDialog.selectBoard("Scrum Board");
        sprintDialog.selectSprint("Sprint 2");

        sprintDialog.insert();

        EditorContent editorContent = editPage.getEditor().getContent();
        editorContent.waitForInlineMacro(JIRA_SPRINT_MACRO_NAME);

        List<MacroPlaceholder> macros = editorContent.macroPlaceholderFor(JIRA_SPRINT_MACRO_NAME);
        assertEquals("incorrect number of inserted macros", 1, macros.size());

        MacroPlaceholder sprintMacro = macros.get(0);
        String[] params = sprintMacro.getAttribute("data-macro-parameters").split("\\|");

        assertEquals("incorrect board id", "board-id=1", params[0]);
        assertEquals("incorrect sprint id", "sprint-id=2", params[2]);
    }
}
