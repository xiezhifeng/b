package it.com.atlassian.confluence.plugins.webdriver.helper;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraRestHelper
{
    private static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    private static final String ISSUE_ENDPOINT = JIRA_BASE_URL + "/rest/api/2/issue";
    private static final String FILTER_ENDPOINT = JIRA_BASE_URL + "/rest/api/2/filter";
    private static final Logger log = LoggerFactory.getLogger(JiraRestHelper.class);

    public static String createIssue(JiraIssueBean jiraIssueBean) throws Exception
    {
        String jsonPayload = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        JsonNode response = RestHelper.postJson(ISSUE_ENDPOINT, jsonPayload, User.ADMIN);
        return JiraUtil.createBasicJiraIssueBeanFromResponse(response.toString()).getId();
    }

    public static void deleteIssue(String id)
    {
        RestHelper.doDeleteJson(ISSUE_ENDPOINT + "/" + id, User.ADMIN);
    }

    public static int deleteJiraFilter(String filterId, HttpClient httpClient)
    {
        int status = 0;

        try
        {
            DeleteMethod method = new DeleteMethod(FILTER_ENDPOINT + "/" + filterId + "?" + getAuthenticationParams());
            status = httpClient.executeMethod(method);
        }
        catch (Exception e)
        {
            log.error("Error deleting JIRA filter", e);
        }

        return status;
    }

    private static String getAuthenticationParams()
    {
        return "os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }
}
