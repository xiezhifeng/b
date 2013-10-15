package it.webdriver.com.atlassian.confluence.helper;

import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;

public class JiraRestHelper
{
    public static final String JIRA_USERNAME = "admin";
    public static final String JIRA_PASSWORD = "admin";

    private static final User JIRA_USER = new User(JIRA_USERNAME, JIRA_PASSWORD, JIRA_USERNAME, "");
    private static final String CREATE_ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";

    public static void createIssue(JiraIssueBean jiraIssueBean) throws JSONException
    {
        String jsonPayload = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        RestHelper.postJson(CREATE_ISSUE_ENDPOINT, jsonPayload, JIRA_USER);
    }
}
