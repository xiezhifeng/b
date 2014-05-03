package it.webdriver.com.atlassian.confluence.jiraissues.recentlyviewpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.test.categories.OnDemandSuiteTest;
import it.webdriver.com.atlassian.confluence.AbstractJiraODWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraLoginPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraRecentlyViewDialog;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(OnDemandSuiteTest.class)
public class JiraRecentlyViewPanelWebDriverTest extends AbstractJiraODWebDriverTest
{

    private JiraRecentlyViewDialog jiraRecentlyViewDialog;

    @After
    public void closeDialog() throws Exception
    {
        closeDialog(jiraRecentlyViewDialog);
    }

    @Test
    public void testRecentlyViewedIssuesAppear() throws Exception
    {
        if(!TestProperties.isOnDemandMode())
        {
            product.getTester().gotoUrl(JIRA_BASE_URL + "/login.jsp");
            JiraLoginPage jiraLoginPage = product.getPageBinder().bind(JiraLoginPage.class);
            jiraLoginPage.login(User.CONF_ADMIN);

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
