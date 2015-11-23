package it.com.atlassian.confluence.plugins.webdriver;


import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;
import it.com.atlassian.confluence.plugins.webdriver.model.JiraProjectModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractJiraODTest extends AbstractJiraTest
{
    private static Backdoor jiraBackdoor = new Backdoor(new TestKitLocalEnvironmentData());
    protected static final JiraProjectModel PROJECT_TOD = new JiraProjectModel("Test OD Project","TOD");
    protected static final JiraProjectModel PROJECT_TZA = new JiraProjectModel("Test OD Project 1","TZA");
    protected static final JiraProjectModel PROJECT_THQ = new JiraProjectModel("Test OD Project 2","THQ");
    protected static String projectLead = "admin";

    @BeforeClass
    public static void init() throws Exception
    {
        creatODTestData();
    }

    @Before
    public void setUp()
    {
        getReadyOnEditTestPage();
    }

    @After
    public void tearDown()
    {
        closeDialog(jiraMacroCreatePanelDialog);
        closeDialog(dialogJiraRecentView);
        closeDialog(dialogPieChart);
        closeDialog(dialogCreatedVsResolvedChart);
        closeDialog(dialogTwoDimensionalChart);
        closeDialog(dialogSearchPanel);
    }

    @AfterClass
    public static void clean() throws Exception {
        cancelEditPage(editPage);
        deleteODTestData();
    }

    public static void creatODTestData() {
        long projectId;
        projectId = jiraBackdoor.project().addProject(PROJECT_TOD.getProjectName(),PROJECT_TOD.getProjectKey(), projectLead);
        PROJECT_TOD.setProjectId(String.valueOf(projectId));
        jiraBackdoor.issues().createIssue(PROJECT_TOD.getProjectKey(), "New Fearture");
        jiraBackdoor.issues().createIssue(PROJECT_TOD.getProjectKey(), "New Fearture");

        projectId = jiraBackdoor.project().addProject(PROJECT_TZA.getProjectName(),PROJECT_TZA.getProjectKey(), projectLead);
        PROJECT_TZA.setProjectId(String.valueOf(projectId));
        jiraBackdoor.issues().createIssue(PROJECT_TZA.getProjectKey(), "New Fearture");
        jiraBackdoor.issues().createIssue(PROJECT_TZA.getProjectKey(), "New Fearture");

        projectId = jiraBackdoor.project().addProject(PROJECT_THQ.getProjectName(),PROJECT_THQ.getProjectKey(), projectLead);
        PROJECT_THQ.setProjectId(String.valueOf(projectId));
        jiraBackdoor.issues().createIssue(PROJECT_THQ.getProjectKey(), "New Bug");


    }

    public static void deleteODTestData(){
        jiraBackdoor.project().deleteProject(PROJECT_TOD.getProjectKey());
        jiraBackdoor.project().deleteProject(PROJECT_TZA.getProjectKey());
        jiraBackdoor.project().deleteProject(PROJECT_THQ.getProjectKey());
    }

}
