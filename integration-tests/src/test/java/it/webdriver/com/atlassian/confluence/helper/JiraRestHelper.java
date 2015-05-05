package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.confluence.json.json.JsonBoolean;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapService;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapServiceServiceLocator;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class JiraRestHelper
{
    private static final String ISSUE_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue";
    private static final String FILTER_ENDPOINT = AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/filter";

    private static JiraSoapService jiraSoapService;
    private static String jiraSoapToken;
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

    // Temporary method used to initialise the JIRA SOAP variables if we are testing against OD instances
    // This can be removed when all the SOAP service calls are replaced with REST calls
    public static void initJiraSoapServices() throws Exception
    {
        // TODO Update to use JIRA's REST API once it supports project creation
        // Create JiraSoapService (only used for project creation)
        // NOTE: JIRA's SOAP and XML-RPC API has already been deprecated as of 6.0 and will be removed in 7.0 but the REST
        // API which replaces SOAP currently does not provide the capability of creating projects
        JiraSoapServiceServiceLocator soapServiceLocator = new JiraSoapServiceServiceLocator();
        soapServiceLocator.setJirasoapserviceV2EndpointAddress(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rpc/soap/jirasoapservice-v2?wsdl");
        jiraSoapService = soapServiceLocator.getJirasoapserviceV2();
        jiraSoapToken = jiraSoapService.login(User.ADMIN.getUsername(), User.ADMIN.getPassword());
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
    public static JiraProjectModel createJiraProject(String projectKey, String projectName, String projectDescription,
                                  String projectUrl, User projectLead, HttpClient httpClient) throws Exception
    {
        jiraSoapService.createProject(
                jiraSoapToken,
                projectKey,
                projectName,
                projectDescription,
                projectUrl,
                projectLead.getUsername(), null, null, null);

        // Store project metadata
        JiraProjectModel jiraProject = new JiraProjectModel();

        GetMethod method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/issue/createmeta?" + JiraRestHelper.getAuthenticationParams() + "&projectKeys=" + projectKey);
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
            method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/greenhopper/1.0/api/epicproperties?" + JiraRestHelper.getAuthenticationParams());
            httpClient.executeMethod(method);

            JSONObject epicProperties = new JSONObject(method.getResponseBodyAsString());
            jiraProject.getProjectEpicProperties().put(EpicProperties.NAME_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.NAME_FIELD.toString()).getString("id"));
            jiraProject.getProjectEpicProperties().put(EpicProperties.STATUS_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.STATUS_FIELD.toString()).getString("id"));
        }

        jiraProject.setProjectKey(projectKey);
        jiraProject.setProjectName(projectName);
        return jiraProject;
    }

    public static void deleteJiraProject(String projectKey, HttpClient httpClient) throws Exception
    {
        GetMethod method = new GetMethod(AbstractJiraWebDriverTest.JIRA_BASE_URL + "/rest/api/2/project/" + projectKey + "?" + JiraRestHelper.getAuthenticationParams());
        if (httpClient.executeMethod(method) == HttpStatus.SC_OK)
        {
            jiraSoapService.deleteProject(jiraSoapToken, projectKey);
        }
    }

    public static String createIssue(JiraIssueBean jiraIssueBean) throws Exception
    {
        String jsonPayload = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        JsonNode response = RestHelper.postJson(ISSUE_ENDPOINT, jsonPayload, User.ADMIN);
        return JiraUtil.createBasicJiraIssueBeanFromResponse(response.toString()).getId();
    }

    public static void createIssues(List<JiraIssueBean> jiraIssueBeans) throws IOException
    {
        JsonArray jsonIssues = new JsonArray();
        for(JiraIssueBean jiraIssueBean: jiraIssueBeans)
        {
            String jiraIssueJson = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
            com.google.gson.JsonObject jsonObject = new JsonParser().parse(jiraIssueJson).getAsJsonObject();
            jsonIssues.add(jsonObject);
        }
        com.google.gson.JsonObject rootIssueJson = new com.google.gson.JsonObject();
        rootIssueJson.add("issueUpdates", jsonIssues);

        RestHelper.postJson(ISSUE_ENDPOINT + "/bulk", rootIssueJson.toString(), User.ADMIN);
    }

    public static void deleteIssue(String id)
    {
        RestHelper.doDeleteJson(ISSUE_ENDPOINT + "/" + id, User.ADMIN);
    }

    public static String createJiraFilter(String name, String jql, String description, HttpClient httpClient)
    {
        JsonObject filter = new JsonObject()
                .setProperty("name", name)
                .setProperty("description", description)
                .setProperty("jql", jql)
                .setProperty("favourite", new JsonBoolean(true));

        try
        {
            PostMethod method = new PostMethod(FILTER_ENDPOINT + "?" + getAuthenticationParams());
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

    public static String getAuthenticationParams()
    {
        return "os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }
}
