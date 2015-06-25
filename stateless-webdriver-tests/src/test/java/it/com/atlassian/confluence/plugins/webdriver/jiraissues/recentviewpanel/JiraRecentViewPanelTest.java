package it.com.atlassian.confluence.plugins.webdriver.jiraissues.recentviewpanel;

import com.atlassian.confluence.plugins.pageobjects.JiraLoginPage;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroRecentPanelDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.test.categories.OnDemandSuiteTest;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
public class JiraRecentViewPanelTest extends AbstractJiraTest
{

    protected JiraMacroRecentPanelDialog dialogJiraRecentView;
    protected static EditContentPage editPage;

    @After
    public void teardown() throws Exception
    {
        closeDialog(dialogJiraRecentView);

        if (editPage != null && editPage.getEditor().isCancelVisibleNow()) {
            editPage.getEditor().clickCancel();
        }

        super.tearDown();
    }

    @Test
    public void testRecentViewIssuesAppear() throws Exception
    {
        // in BTF, we need to login JIRA first to access some JIRA issues.
        if(!TestProperties.isOnDemandMode())
        {
            product.getTester().gotoUrl(JIRA_BASE_URL + "/login.jsp");
            JiraLoginPage jiraLoginPage = pageBinder.bind(JiraLoginPage.class);
            jiraLoginPage.login(UserWithDetails.CONF_ADMIN);
        }

        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TP-1");

        editPage = gotoEditTestPage(UserWithDetails.CONF_ADMIN);

        dialogJiraRecentView = openJiraMacroRecentPanelDialog();

        assertTrue(dialogJiraRecentView.isResultContainIssueKey("TP-1"));
    }

    protected JiraMacroRecentPanelDialog openJiraMacroRecentPanelDialog() throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialog.selectMenuItem("Recently Viewed");

        waitForAjaxRequest();

        dialogJiraRecentView = pageBinder.bind(JiraMacroRecentPanelDialog.class);

        return dialogJiraRecentView;
    }
}
