package it.webdriver.com.atlassian.confluence.helper;

import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;

import java.io.IOException;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;

public class JiraRestHelper
{
    public static final String JIRA_USERNAME = "admin";
    public static final String JIRA_PASSWORD = "admin";

    private static final User JIRA_USER = new User(JIRA_USERNAME, JIRA_PASSWORD, JIRA_USERNAME, "");
    private static final String CREATE_ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";
    private static final String DELETE_ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";

    public static String createIssue(JiraIssueBean jiraIssueBean) throws JSONException, IOException
    {
        String jsonPayload = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        JSONObject response = RestHelper.postJson(CREATE_ISSUE_ENDPOINT, jsonPayload, JIRA_USER);
        return JiraUtil.createBasicJiraIssueBeanFromResponse(response.toString()).getId();
    }

    public static void deleteIssue(String id)
    {
        RestHelper.doDeleteJson(DELETE_ISSUE_ENDPOINT + "/" + id, JIRA_USER);
    }

}
