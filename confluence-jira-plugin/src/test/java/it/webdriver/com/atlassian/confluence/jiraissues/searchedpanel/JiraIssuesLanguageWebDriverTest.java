package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraGeneralConfigurationPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraLoginPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class JiraIssuesLanguageWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @After
    public void resetLanguage()
    {
        changeJiraLanguage("-1");
    }

    @Test
    public void testNewJimWhenChangeJiraDefaultLanguage()
    {
        changeJiraLanguage("fr_FR");
        product.loginAndEdit(User.ADMIN, Page.TEST);
        openJiraIssuesDialog();

        jiraIssuesDialog.inputJqlSearch("project = tstt");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.typeSelect2Input("Date Cus");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.getIssuesTableElement().getText().contains("Dec 25, 2009"));
    }

    @Test
    public void testVerifyExistingJimWhenChangeDefaultLanguage()
    {
        convertToMacroPlaceholder("{jiraissues:project = tstt|cache=off|columns=key,type,created,updated,due,status,fixversions,date customfield}");
        waitUntilInlineMacroAppearsInEditor(editContentPage, OLD_JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        changeJiraLanguage("fr_FR");

        product.loginAndView(User.ADMIN, Page.TEST);
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        String tableContent = jiraIssuesPage.getIssuesTableElement().getText();
        Assert.assertTrue(tableContent.contains("Ouvertes"));
        Assert.assertTrue(tableContent.contains("date customfield"));
        Assert.assertTrue(tableContent.contains("Dec 25, 2009"));
    }

    private void changeJiraLanguage(String language)
    {
        product.getTester().gotoUrl(JIRA_BASE_URL + "/login.jsp");
        JiraLoginPage jiraLoginPage = product.getPageBinder().bind(JiraLoginPage.class);
        jiraLoginPage.login(User.ADMIN);

        product.getTester().gotoUrl(JIRA_BASE_URL + "/secure/admin/EditApplicationProperties!default.jspa");
        JiraGeneralConfigurationPage configurationPage = product.getPageBinder().bind(JiraGeneralConfigurationPage.class);
        configurationPage.selectLanguage(language);
        configurationPage.clickUpdateButton();
    }
}
