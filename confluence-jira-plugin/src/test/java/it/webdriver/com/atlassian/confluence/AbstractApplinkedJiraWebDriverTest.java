package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapService;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapServiceServiceLocator;
import it.webdriver.com.atlassian.confluence.model.JiraProjectModel;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractApplinkedJiraWebDriverTest extends AbstractApplinkedWebDriverTest
{
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

    private HttpClient httpClient = new HttpClient();
    private JiraSoapService jiraSoapService;
    private String jiraSoapToken;

    protected Map<String, JiraProjectModel> jiraProjects = new HashMap<String, JiraProjectModel>();

    private static final Logger log = LoggerFactory.getLogger(AbstractApplinkedJiraWebDriverTest.class);

    @Before
    public void initJiraSoapService() throws Exception
    {
        // TODO Update to use JIRA's REST API once it supports project creation
        // Create JiraSoapService (only used for project creation)
        // NOTE: JIRA's SOAP and XML-RPC API has already been deprecated as of 6.0 and will be removed in 7.0 but the REST
        // API which replaces SOAP currently does not provide the capability of creating projects
        JiraSoapServiceServiceLocator soapServiceLocator = new JiraSoapServiceServiceLocator();
        soapServiceLocator.setJirasoapserviceV2EndpointAddress(jiraBaseUrl + "/rpc/soap/jirasoapservice-v2?wsdl");
        jiraSoapService = soapServiceLocator.getJirasoapserviceV2();
        jiraSoapToken = jiraSoapService.login(User.ADMIN.getUsername(), User.ADMIN.getPassword());
    }

    @After
    public void tearDown() throws Exception
    {
        Iterator<JiraProjectModel> projectIterator = jiraProjects.values().iterator();
        while (projectIterator.hasNext())
        {
            deleteJiraProject(projectIterator.next().getProjectKey());
        }

        serverStateManager.removeTestData();
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
    public void createJiraProject(String projectKey, String projectName, String projectDescription,
                                  String projectUrl, User projectLead) throws Exception
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

        GetMethod method = new GetMethod(jiraBaseUrl + "/rest/api/2/issue/createmeta?" + getAuthenticationParams() + "&projectKeys=" + projectKey);
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
            method = new GetMethod(jiraBaseUrl + "/rest/greenhopper/1.0/api/epicproperties?" + getAuthenticationParams());
            httpClient.executeMethod(method);

            JSONObject epicProperties = new JSONObject(method.getResponseBodyAsString());
            jiraProject.getProjectEpicProperties().put(EpicProperties.NAME_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.NAME_FIELD.toString()).getString("id"));
            jiraProject.getProjectEpicProperties().put(EpicProperties.STATUS_FIELD.toString(), epicProperties.getJSONObject(EpicProperties.STATUS_FIELD.toString()).getString("id"));
        }

        jiraProject.setProjectKey(projectKey);
        jiraProject.setProjectName(projectName);
        jiraProjects.put(projectName, jiraProject);
    }

    public void deleteJiraProject(String projectKey) throws Exception
    {
        GetMethod method = new GetMethod(jiraBaseUrl + "/rest/api/2/project/" + projectKey + "?" + getAuthenticationParams());
        if (httpClient.executeMethod(method) == HttpStatus.SC_OK)
        {
            jiraSoapService.deleteProject(jiraSoapToken, projectKey);
        }
    }

    private String getAuthenticationParams()
    {
        return "os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    protected String createJiraIssue(String issueSummary, IssueType type, String projectName)
    {
        JiraProjectModel jiraProject = jiraProjects.get(projectName);

        JsonObject project = (new JsonObject()).setProperty("id", jiraProject.getProjectId());
        JsonObject issueType = (new JsonObject()).setProperty("id", jiraProject.getProjectIssueTypes().get(type.toString()));

        JsonObject issueFields = new JsonObject();
        issueFields.setProperty("project", project);
        issueFields.setProperty("summary", issueSummary);
        issueFields.setProperty("issuetype", issueType);
        if (type == IssueType.EPIC)
        {
            issueFields.setProperty("customfield_" + jiraProject.getProjectEpicProperties().get(EpicProperties.NAME_FIELD.toString()), issueSummary);
        }

        JsonObject jiraIssue = new JsonObject();
        jiraIssue.setProperty("fields", issueFields);

        try
        {
            PostMethod method = new PostMethod(jiraBaseUrl + "/rest/api/2/issue?" + getAuthenticationParams());
            method.setRequestHeader("Accept", "application/json");
            method.setRequestEntity(new StringRequestEntity(jiraIssue.serialize(), "application/json", "UTF-8"));
            httpClient.executeMethod(method);

            JSONObject response = new JSONObject(method.getResponseBodyAsString());
            return response.getString("key");
        }
        catch (Exception e)
        {
            log.error("Error creating JIRA issue", e);
            return null;
        }
    }

    protected void deleteJiraIssue(String issueKey)
    {
        DeleteMethod method = new DeleteMethod(jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "?" + getAuthenticationParams());
        try
        {
            httpClient.executeMethod(method);
        }
        catch (Exception e)
        {
            log.error("Error deleting JIRA issue");
        }
    }

    protected List<String> createRemoteIssueLinks(List<String> issueKeys, Page page)
    {
        List<String> remoteLinkIds = new ArrayList<String>();

        for (String issueKey : issueKeys)
        {
            JsonObject remoteLink = new JsonObject()
                    .setProperty("globalId", "appId=" + applinkIds.get(confBaseUrl) + "&pageId=" + page.getIdAsString())
                    .setProperty("application", new JsonObject()
                            .setProperty("type", "com.atlassian.confluence")
                            .setProperty("name", "testconfluence"))
                    .setProperty("relationship", "mentioned in")
                    .setProperty("object", new JsonObject()
                            .setProperty("url", confBaseUrl + page.getUrl())
                            .setProperty("title", page.getTitle()));

            try
            {
                PostMethod method = new PostMethod(jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "/remotelink?" + getAuthenticationParams());
                method.setRequestHeader("Accept", "application/json");
                method.setRequestEntity(new StringRequestEntity(remoteLink.serialize(), "application/json", "UTF-8"));
                httpClient.executeMethod(method);

                JSONObject response = new JSONObject(method.getResponseBodyAsString());
                remoteLinkIds.add(response.getString("id"));
            }
            catch (Exception e)
            {
                log.error("Error creating remote issue link for issue" + issueKey, e);
            }
        }

        return remoteLinkIds;
    }
}
