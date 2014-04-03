package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Group;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;
import org.junit.After;
import org.junit.Before;

import java.util.*;

import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createJiraProject;

public abstract class AbstractJiraSLATWebDriverTest extends AbstractJiraWebDriverTest
{

    protected static final String PROJECT_TSTT = "Project TSTT Name";
    protected static final String PROJECT_TST = "Project TST Name";
    protected static final String PROJECT_TP = "Project TP Name";

    private static final int PROJECT_TSTT_ISSUE_COUNT = 5;
    private static final int PROJECT_TST_ISSUE_COUNT = 1;
    private static final int PROJECT_TP_ISSUE_COUNT = 2;

    protected Map<String, JiraProjectModel> jiraProjects = new HashMap<String, JiraProjectModel>();

    @Before
    public void initOnDemandData() throws Exception
    {
        if(TestProperties.isOnDemandMode())
        {
            initUser();
            JiraRestHelper.initJiraSoapServices();
            initTestProjects();
            initTestIssues();
        }
    }

    @After
    public void cleanOnDemanData() throws Exception
    {
        if(TestProperties.isOnDemandMode())
        {
            removeTestProjects();
        }
    }

    protected void removeTestProjects() throws Exception
    {
        Iterator<JiraProjectModel> projectIterator = jiraProjects.values().iterator();
        while (projectIterator.hasNext())
        {
            JiraRestHelper.deleteJiraProject(projectIterator.next().getProjectKey(), client);
        }
    }

    protected void initUser() throws Exception
    {
        // Hack - set correct user group while UserManagementHelper is still being fixed (CONFDEV-20880). This logic should be handled by using Group.USERS
        Group userGroup = TestProperties.isOnDemandMode() ? Group.ONDEMAND_ALACARTE_USERS : Group.CONF_ADMINS;

        // Setup User.ADMIN to have all permissions
        userHelper.createGroup(Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, userGroup);

        userHelper.synchronise();
        // Hack - the synchronise method doesn't actually sync the directory on OD so we just need to wait... Should also be addressed in CONFDEV-20880
        Thread.sleep(10000);
    }

    protected void initTestProjects() throws Exception
    {
        jiraProjects.put(PROJECT_TSTT, createJiraProject("TSTT", PROJECT_TSTT, "", "", User.ADMIN, client));
        jiraProjects.put(PROJECT_TST, createJiraProject("TST", PROJECT_TST, "", "", User.ADMIN, client));
        jiraProjects.put(PROJECT_TP, createJiraProject("TP", PROJECT_TP, "", "", User.ADMIN, client));
    }

    protected void initTestIssues() throws Exception
    {
        List<JiraIssueBean> jiraIssueBeans = new ArrayList<JiraIssueBean>();
        for (int i = 0; i < PROJECT_TSTT_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    jiraProjects.get(PROJECT_TSTT).getProjectId(),
                    jiraProjects.get(PROJECT_TSTT).getProjectIssueTypes().get(JiraRestHelper.IssueType.BUG.toString()),
                    "test", ""));
        }

        for (int i = 0; i < PROJECT_TST_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    jiraProjects.get(PROJECT_TST).getProjectId(),
                    jiraProjects.get(PROJECT_TST).getProjectIssueTypes().get(JiraRestHelper.IssueType.TASK.toString()),
                    "test", ""));
        }

        for (int i = 0; i < PROJECT_TP_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    jiraProjects.get(PROJECT_TP).getProjectId(),
                    jiraProjects.get(PROJECT_TP).getProjectIssueTypes().get(JiraRestHelper.IssueType.NEW_FEATURE.toString()),
                    "test", ""));
        }

        JiraRestHelper.createIssues(jiraIssueBeans);
    }
}
