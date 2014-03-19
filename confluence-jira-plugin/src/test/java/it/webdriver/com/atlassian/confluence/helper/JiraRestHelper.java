package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.confluence.json.json.JsonBoolean;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapService;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;

import java.io.IOException;
import java.util.Map;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraRestHelper
{
    public static final String JIRA_USERNAME = "admin";
    public static final String JIRA_PASSWORD = "admin";

    private static final User JIRA_USER = new User(JIRA_USERNAME, JIRA_PASSWORD, JIRA_USERNAME, "");
    private static final String CREATE_ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";
    private static final String DELETE_ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";

    private static final HttpClient httpClient = new HttpClient();
    private static final Logger log = LoggerFactory.getLogger(JiraRestHelper.class);

    public enum IssueType
    {
        BUG("Bug"),
        NEW_FEATURE("New Feature"),
        TASK("Task"),
        IMPROVEMENT("Improvement"),
        SUB_TASK("Sub-task"),
        EPIC("Epic"),
        STORY("Story"),
        TECHNICAL_TASK("Technical Task");

        private String name;

        private IssueType(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public enum EpicProperties
    {
        NAME_FIELD("epicNameField"),
        STATUS_FIELD("epicStatusField");

        private String name;

        private EpicProperties(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    /**
     * Creates a JIRA project with the default permission scheme
     * @param projectKey
     * @param projectName
     * @param projectDescription
     * @param projectUrl
     * @param projectLead
     * @throws Exception
     */
    public static void createJiraProject(String projectKey, String projectName, String projectDescription,
                                     String projectUrl, User projectLead, JiraSoapService jiraSoapService, String jiraSoapToken,
                                     Map<String, JiraProjectModel> jiraProjects)
    {
        try
        {
            jiraSoapService.createProject(
                    jiraSoapToken,
                    projectKey,
                    projectName,
                    projectDescription,
                    projectUrl,
                    projectLead.getUsername(), null, null, null);
        }
        catch (Exception e)
        {
            log.error("Error creating JIRA project " + projectKey, e);
        }

        // Store project metadata
        JiraProjectModel jiraProject = new JiraProjectModel();

        try
        {
            GetMethod method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue/createmeta?" + getAuthenticationParams() + "&projectKeys=" + projectKey);
            httpClient.executeMethod(method);

            JSONObject jsonProjectMetadata = (new JSONObject(method.getResponseBodyAsString())).getJSONArray("projects").getJSONObject(0);
            jiraProject.setProjectId(jsonProjectMetadata.getString("id"));
            JSONArray issueTypes = jsonProjectMetadata.getJSONArray("issuetypes");

            for (int i = 0; i < issueTypes.length(); i++)
            {
                JSONObject issueType = issueTypes.getJSONObject(i);
                jiraProject.getProjectIssueTypes().put(issueType.getString("name"), issueType.getString("id"));
            }

            // Retrieve epic properties (if applicable)
            if (jiraProject.getProjectIssueTypes().containsKey(IssueType.EPIC.toString()))
            {
                method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/greenhopper/1.0/api/epicproperties?" + getAuthenticationParams());
                httpClient.executeMethod(method);

                JSONObject epicProperties = new JSONObject(method.getResponseBodyAsString());
                jiraProject.getProjectEpicProperties().put(EpicProperties.NAME_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.NAME_FIELD.toString()).getString("id"));
                jiraProject.getProjectEpicProperties().put(EpicProperties.STATUS_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.STATUS_FIELD.toString()).getString("id"));
            }
        }
        catch (Exception e)
        {
            log.error("Error retrieving metadata for JIRA project " + projectKey, e);
        }

        jiraProject.setProjectKey(projectKey);
        jiraProject.setProjectName(projectName);
        jiraProjects.put(projectName, jiraProject);
    }

    public static void deleteJiraProject(String projectKey, JiraSoapService jiraSoapService, String jiraSoapToken) throws Exception
    {
        GetMethod method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/project/" + projectKey + "?" + getAuthenticationParams());
        if (httpClient.executeMethod(method) == HttpStatus.SC_OK)
        {
            jiraSoapService.deleteProject(jiraSoapToken, projectKey);
        }
    }

    public static String createIssue(JiraIssueBean jiraIssueBean) throws JSONException, IOException
    {
        String jsonPayload = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        JsonNode response = RestHelper.postJson(CREATE_ISSUE_ENDPOINT, jsonPayload, JIRA_USER);
        return JiraUtil.createBasicJiraIssueBeanFromResponse(response.toString()).getId();
    }

    public static void deleteIssue(String id)
    {
        RestHelper.doDeleteJson(DELETE_ISSUE_ENDPOINT + "/" + id, JIRA_USER);
    }

    protected String createJiraFilter(String name, String jql, String description)
    {
        JsonObject filter = new JsonObject()
                .setProperty("name", name)
                .setProperty("description", description)
                .setProperty("jql", jql)
                .setProperty("favourite", new JsonBoolean(true));

        try
        {
            PostMethod method = new PostMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/filter?" + getAuthenticationParams());
            method.setRequestHeader("Accept", "application/json");
            method.setRequestEntity(new StringRequestEntity(filter.serialize(), "application/json", "UTF-8"));
            httpClient.executeMethod(method);

            JSONObject response = new JSONObject(method.getResponseBodyAsString());
            return response.getString("id");
        }
        catch (Exception e)
        {
            log.error("Error creating JIRA filter", e);
            return null;
        }
    }

    private static String getAuthenticationParams()
    {
        return "os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }
}
