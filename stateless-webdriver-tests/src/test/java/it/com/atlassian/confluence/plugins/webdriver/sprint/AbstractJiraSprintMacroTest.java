package it.com.atlassian.confluence.plugins.webdriver.sprint;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import it.com.atlassian.confluence.plugins.webdriver.model.BoardModel;
import it.com.atlassian.confluence.plugins.webdriver.model.KanbanBoardModel;
import it.com.atlassian.confluence.plugins.webdriver.model.ScrumBoardModel;
import it.com.atlassian.confluence.plugins.webdriver.model.SprintModel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroPage;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import static it.com.atlassian.confluence.plugins.webdriver.model.SprintStatus.*;

public class AbstractJiraSprintMacroTest extends AbstractJiraTest
{
    protected static final BoardModel SCRUM_BOARD_1 = new ScrumBoardModel("Scrum Board 1");
    protected static final SprintModel SPRINT1 = new SprintModel("Sprint 1", CLOSED, SCRUM_BOARD_1);
    protected static final SprintModel SPRINT2 = new SprintModel("Sprint 2", ACTIVE, SCRUM_BOARD_1);
    protected static final SprintModel SPRINT3 = new SprintModel("Sprint 3", FUTURE, SCRUM_BOARD_1);

    protected static final BoardModel SCRUM_BOARD_2 = new ScrumBoardModel("Scrum Board 2");
    protected static final BoardModel KANBAN_BOARD = new KanbanBoardModel("Kanban Board");

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
        sprintDialog = openSprintDialogFromMacroBrowser(editPage);
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(sprintDialog);
    }

    protected JiraSprintMacroPage createSprintPage(BoardModel board, SprintModel sprint)
    {
        sprintDialog.selectBoard(board.getName());
        sprintDialog.selectSprint(sprint.getName());

        sprintDialog.insert();
        editPage.save();

        return bindCurrentPageToSprintPage();
    }
}
