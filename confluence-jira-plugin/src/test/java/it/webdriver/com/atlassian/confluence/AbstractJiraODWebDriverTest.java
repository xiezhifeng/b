package it.webdriver.com.atlassian.confluence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;

import org.junit.After;
import org.junit.Before;

import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;

import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createJiraProject;

public abstract class AbstractJiraODWebDriverTest extends AbstractJiraWebDriverTest
{

    protected static final String PROJECT_TSTT = "Test Project";
    protected static final String PROJECT_TP = "Test Project 1";
    protected static final String PROJECT_TST = "Test Project 2";

    private static final int PROJECT_TSTT_ISSUE_COUNT = 5;
    private static final int PROJECT_TST_ISSUE_COUNT = 1;
    private static final int PROJECT_TP_ISSUE_COUNT = 2;

    protected Map<String, JiraProjectModel> onDemandJiraProjects = new HashMap<String, JiraProjectModel>();

    protected Map<String, String> internalJiraProjects = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(PROJECT_TSTT, "10011");
            put(PROJECT_TP, "10000");
            put(PROJECT_TST, "10010");
        }
    });


    @Before
    public void initOnDemandData() throws Exception
    {
        if(TestProperties.isOnDemandMode())
        {
            //initUser();
            JiraRestHelper.initJiraSoapServices();
            initTestProjects();
            initTestIssues();
        }
    }

    @After
    public void cleanOnDemandData() throws Exception
    {
        if(TestProperties.isOnDemandMode())
        {
            removeTestProjects();
        }
    }

    protected void removeTestProjects() throws Exception
    {
        Iterator<JiraProjectModel> projectIterator = onDemandJiraProjects.values().iterator();
        while (projectIterator.hasNext())
        {
            JiraRestHelper.deleteJiraProject(projectIterator.next().getProjectKey(), client);
        }
    }

    /*
    protected void initUser() throws Exception
    {
        // Hack - set correct user group while UserManagementHelper is still being fixed (CONFDEV-20880). This logic should be handled by using Group.USERS
        Group userGroup = TestProperties.isOnDemandMode() ? Group.ONDEMAND_ALACARTE_USERS : Group.CONF_ADMINS;

        // Setup User.ADMIN to have all permissions
        if (!TestProperties.isOnDemandMode())
        {
            userHelper.createGroup(Group.DEVELOPERS);
        }
        // CONFDEV-24400 add OnDemand sysadmin user to jira-users and jira-developers groups
        // we need to create these groups in Crowd first
        userHelper.createGroup(JIRA_USERS);
        userHelper.createGroup(JIRA_DEVELOPERS);
        // then we add sysadmin to these groups
        userHelper.addUserToGroup(User.ADMIN, JIRA_DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, JIRA_USERS);
        userHelper.addUserToGroup(User.ADMIN, userGroup);

        userHelper.synchronise();
        // Hack - the synchronise method doesn't actually sync the directory on OD so we just need to wait... Should also be addressed in CONFDEV-20880
        //Thread.sleep(10000);
    }
    */

    protected void initTestProjects() throws Exception
    {
        onDemandJiraProjects.put(PROJECT_TSTT, createJiraProject("TSTT", PROJECT_TSTT, "", "", User.ADMIN, client));
        onDemandJiraProjects.put(PROJECT_TST, createJiraProject("TST", PROJECT_TST, "", "", User.ADMIN, client));
        onDemandJiraProjects.put(PROJECT_TP, createJiraProject("TP", PROJECT_TP, "", "", User.ADMIN, client));
    }

    protected void initTestIssues() throws Exception
    {
        List<JiraIssueBean> jiraIssueBeans = new ArrayList<JiraIssueBean>();
        for (int i = 0; i < PROJECT_TSTT_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    getProjectId(PROJECT_TSTT),
                    onDemandJiraProjects.get(PROJECT_TSTT).getProjectIssueTypes().get(JiraRestHelper.IssueType.BUG.toString()),
                    "test", ""));
        }

        for (int i = 0; i < PROJECT_TST_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    getProjectId(PROJECT_TST),
                    onDemandJiraProjects.get(PROJECT_TST).getProjectIssueTypes().get(JiraRestHelper.IssueType.TASK.toString()),
                    "test", ""));
        }

        for (int i = 0; i < PROJECT_TP_ISSUE_COUNT; i++)
        {
            jiraIssueBeans.add(new JiraIssueBean(
                    getProjectId(PROJECT_TP),
                    onDemandJiraProjects.get(PROJECT_TP).getProjectIssueTypes().get(JiraRestHelper.IssueType.NEW_FEATURE.toString()),
                    "test", ""));
        }

        JiraRestHelper.createIssues(jiraIssueBeans);
    }

    protected String getProjectId(String projectName)
    {
        if(TestProperties.isOnDemandMode())
        {
            return onDemandJiraProjects.get(projectName).getProjectId();
        }

        return internalJiraProjects.get(projectName);
    }
}
