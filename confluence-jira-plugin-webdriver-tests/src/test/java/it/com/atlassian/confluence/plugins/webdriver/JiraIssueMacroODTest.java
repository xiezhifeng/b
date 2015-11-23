package it.com.atlassian.confluence.plugins.webdriver;

import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.test.categories.OnDemandAcceptanceTest;
import com.atlassian.test.categories.OnDemandSuiteTest;
import com.atlassian.webdriver.utils.by.ByJquery;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraLoginPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroRecentPanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({OnDemandAcceptanceTest.class, OnDemandSuiteTest.class})
@Ignore("CONFDEV-37479")
public class JiraIssueMacroODTest extends AbstractJiraODTest{

    protected static final String NO_ISSUES_COUNT_TEXT = "No issues found";
    protected static final String ONE_ISSUE_COUNT_TEXT = "1 issue";
    protected static final String MORE_ISSUES_COUNT_TEXT = "issues";
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    @Test
    public void testCreateIssue() throws Exception
    {
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(PROJECT_TOD.getProjectId());

        jiraMacroCreatePanelDialog.selectProject(PROJECT_TOD.getProjectName());
        jiraMacroCreatePanelDialog.getSummaryElement().type("summary");

        EditContentPage editContentPage = jiraMacroCreatePanelDialog.insertIssue();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
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

        product.getTester().gotoUrl(JIRA_BASE_URL + "/browse/TOD-1");

        editPage = gotoEditTestPage(UserWithDetails.CONF_ADMIN);

        dialogJiraRecentView = openJiraMacroRecentPanelDialog();

        assertTrue(dialogJiraRecentView.isResultContainIssueKey("TOD-1"));
    }

    @Test
    public void testNoIssuesCountText() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("status=Reopened");
        assertEquals(NO_ISSUES_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testOneIssueResultText() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("project = THQ");
        assertEquals(ONE_ISSUE_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testMoreIssueResultText() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("status=Open");
        assertTrue(jiraIssuesPage.getNumberOfIssuesText().contains(MORE_ISSUES_COUNT_TEXT));
    }

    @Test
    public void validateMacroInContentPage()
    {
        openPieChartAndSearch().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(By.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }

    @Test
    public void validateCreatedVsResolvedMacroInContentPage()
    {
        openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }

    @Test
    public void testTwoDimensionalChartData()
    {
        dialogTwoDimensionalChart = openTwoDimensionalChartDialog();
        dialogTwoDimensionalChart.inputJqlSearch("project=TOD");
        dialogTwoDimensionalChart.clickPreviewButton();
        assertTrue(dialogTwoDimensionalChart.isChartImageVisible());

        EditContentPage editContentPage = dialogTwoDimensionalChart.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);
        editContentPage.getEditor().clickSaveAndWaitForPageChange();

        pageJiraChartView = pageBinder.bind(JiraChartViewPage.class);
        assertTrue(pageJiraChartView.getChartSummary().getText().contains("Showing 1 of 1 statistics"));
        assertTrue(pageJiraChartView.getXAxis().equals("Status"));
        assertTrue(pageJiraChartView.getYAxis().equals("Assignee"));
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
