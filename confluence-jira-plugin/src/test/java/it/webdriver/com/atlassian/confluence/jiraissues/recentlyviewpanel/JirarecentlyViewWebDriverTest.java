package it.webdriver.com.atlassian.confluence.jiraissues.recentlyviewpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.test.categories.OnDemandSuiteTest;
import it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel.AbstractJiraIssuesSearchPanelWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraLoginPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraRecentlyViewDialog;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.*;

@Category(OnDemandSuiteTest.class)
public class JiraRecentlyViewWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    private JiraRecentlyViewDialog jiraRecentlyViewDialog;

    private final String PROJECT_TSTT = "Project TSTT Name";
    private final String PROJECT_TST = "Project TST Name";
    private final String PROJECT_TP = "Project TP Name";

    private final int PROJECT_TSTT_ISSUE_COUNT = 5;
    private final int PROJECT_TST_ISSUE_COUNT = 1;
    private final int PROJECT_TP_ISSUE_COUNT = 2;

    @Before
    public void setUpJiraTestData() throws Exception
    {
        if (TestProperties.isOnDemandMode())
        {

            jiraProjects.put(PROJECT_TSTT, createJiraProject("TSTT", PROJECT_TSTT, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TST, createJiraProject("TST", PROJECT_TST, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TP, createJiraProject("TP", PROJECT_TP, "", "", User.ADMIN, client));

            for (int i = 0; i < PROJECT_TSTT_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TSTT).getProjectId(),
                        jiraProjects.get(PROJECT_TSTT).getProjectIssueTypes().get(IssueType.BUG.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TST_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TST).getProjectId(),
                        jiraProjects.get(PROJECT_TST).getProjectIssueTypes().get(IssueType.TASK.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TP_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TP).getProjectId(),
                        jiraProjects.get(PROJECT_TP).getProjectIssueTypes().get(IssueType.NEW_FEATURE.toString()),
                        "test", "")));
            }
        }
    }

    @Test
    public void testRecentlyViewedIssuesAppear() throws Exception
    {
        if(!TestProperties.isOnDemandMode())
        {
            product.getTester().gotoUrl(JIRA_BASE_URL + "/login.jsp");
            JiraLoginPage jiraLoginPage = product.getPageBinder().bind(JiraLoginPage.class);
            jiraLoginPage.login(User.ADMIN);

        }
        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TP-1");
        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TP-2");
        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TST-1");
        product.getTester().gotoUrl(WebDriverConfiguration.getBaseUrl() + Page.TEST.getEditUrl());

        JiraRecentlyViewDialog dialog = openJiraRecentlyViewDialog();

        Assert.assertTrue(dialog.isResultContainIssueKey("TP-1"));
        Assert.assertTrue(dialog.isResultContainIssueKey("TP-2"));
        Assert.assertTrue(dialog.isResultContainIssueKey("TST-1"));
    }

    protected JiraRecentlyViewDialog openJiraRecentlyViewDialog() throws Exception
    {
        editContentPage.openInsertMenu();
        jiraRecentlyViewDialog = product.getPageBinder().bind(JiraRecentlyViewDialog.class);
        jiraRecentlyViewDialog.open();
        jiraRecentlyViewDialog.selectMenuItem("Recently Viewed");
        waitForAjaxRequest(product.getTester().getDriver());
        return jiraRecentlyViewDialog;
    }
}
