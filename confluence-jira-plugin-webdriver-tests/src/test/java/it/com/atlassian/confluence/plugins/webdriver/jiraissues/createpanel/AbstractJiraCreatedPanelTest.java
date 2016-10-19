package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractJiraCreatedPanelTest extends AbstractJiraTest
{
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
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(jiraMacroCreatePanelDialog);
    }

    @AfterClass
    public static void clean() throws Exception
    {
        cancelEditPage(editPage);
    }

}
