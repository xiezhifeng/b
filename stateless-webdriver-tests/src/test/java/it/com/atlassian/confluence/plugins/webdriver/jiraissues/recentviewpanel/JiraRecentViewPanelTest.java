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
import org.openqa.selenium.By;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraWebDriverTest;

import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
public class JiraRecentViewPanelTest extends AbstractJiraWebDriverTest
{

    protected JiraMacroRecentPanelDialog dialogJiraRecentView;
    protected static EditContentPage editPage;

    @After
    public void teardown() throws Exception
    {
        closeDialog(dialogJiraRecentView);
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

        editPage = gotoEditTestPage(UserWithDetails.CONF_ADMIN);

        dialogJiraRecentView = openJiraRecentViewDialog();

        assertTrue(dialogJiraRecentView.isResultContainIssueKey("TP-1"));
    }

    protected JiraMacroRecentPanelDialog openJiraRecentViewDialog() throws Exception
    {
        JiraMacroRecentPanelDialog dialogJiraRecentView;

        editPage.getEditor().openInsertMenu().getPageElement().find(By.id("jiralink")).click();
        dialogJiraRecentView = product.getPageBinder().bind(JiraMacroRecentPanelDialog.class);
        dialogJiraRecentView.selectMenuItem("Recently Viewed");

        waitForAjaxRequest(product.getTester().getDriver());

        return dialogJiraRecentView;
    }
}
