package it.com.atlassian.confluence.plugins.webdriver.jiraissues.recentviewpanel;

import com.atlassian.confluence.plugins.pageobjects.JiraLoginPage;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroRecentPanelDialog;
import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.test.categories.OnDemandSuiteTest;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraWebDriverTest;

import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
public class JiraRecentViewPanelTest extends AbstractJiraWebDriverTest
{

    protected JiraMacroRecentPanelDialog jiraRecentViewDialog;
    protected static EditContentPage editPage;

    @After
    public void teardown() throws Exception
    {
        closeDialog(jiraRecentViewDialog);
    }

    @Test
    public void testRecentViewIssuesAppear() throws Exception
    {
        // in BTF, we need to login JIRA first to access some JIRA issues.
        if(!TestProperties.isOnDemandMode())
        {
            product.getTester().gotoUrl(JIRA_BASE_URL + "/login.jsp");
            JiraLoginPage jiraLoginPage = product.getPageBinder().bind(JiraLoginPage.class);
            jiraLoginPage.login(UserWithDetails.CONF_ADMIN);
        }

        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TP-1");
        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TP-2");

        editPage = gotoEditTestPage(UserWithDetails.CONF_ADMIN);

        jiraRecentViewDialog = openJiraRecentViewDialog();

        assertTrue(jiraRecentViewDialog.isResultContainIssueKey("TP-1"));
        assertTrue(jiraRecentViewDialog.isResultContainIssueKey("TP-2"));
    }

    protected JiraMacroRecentPanelDialog openJiraRecentViewDialog() throws Exception
    {
        JiraMacroRecentPanelDialog jiraRecentViewDialog;

        editPage.getEditor().openInsertMenu();

        jiraRecentViewDialog = product.getPageBinder().bind(JiraMacroRecentPanelDialog.class);
        jiraRecentViewDialog.open();
        jiraRecentViewDialog.selectMenuItem("Recently Viewed");

        waitForAjaxRequest(product.getTester().getDriver());

        return jiraRecentViewDialog;
    }
}
