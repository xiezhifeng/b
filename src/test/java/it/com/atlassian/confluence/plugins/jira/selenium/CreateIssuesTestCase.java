package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@SuppressWarnings("deprecation")
public class CreateIssuesTestCase extends AbstractJiraPanelTestCase
{
    public void testCreateIssues() throws JsonParseException, JsonMappingException, IOException,
            com.atlassian.confluence.json.parser.JSONException
    {
        String serverId = getDefaultServerId();
        assertTrue(StringUtils.isNotEmpty(serverId));

        List<JiraIssueBean> jiraIssueBeans = createJiraIssueBeans();
        String jiraIssueBeansJsonPayload = createJiraIssuesJsonPayload(jiraIssueBeans);

        String createIssueRestUrl = getConfluenceWebTester().getBaseUrl() + "/rest/jiraanywhere/1.0"
                + "/jira-issue/create-jira-issues/" + serverId;

        WebResource.Builder resource = RestHelper.newJsonResource(createIssueRestUrl, User.ADMIN);
        ClientResponse response = resource.post(ClientResponse.class, jiraIssueBeansJsonPayload);

        JSONArray jiraIssueJsonResults = new JSONArray(response.getEntity(String.class));
        for (int i = 0; i < jiraIssueJsonResults.length(); ++i)
        {
            ObjectMapper jiraIssueBeanMapper = new ObjectMapper();
            String test = jiraIssueJsonResults.getJSONObject(i).toString();
            JiraIssueBean jiraIssueBeanResult = jiraIssueBeanMapper.readValue(test, JiraIssueBean.class);
            validate(jiraIssueBeanResult);
        }
    }

    /**
     * Validate jira issue information in JIRA is matched with input information
     * 
     * @param jiraIssueBeanResult
     * @throws com.atlassian.confluence.json.parser.JSONException
     */
    private void validate(JiraIssueBean jiraIssueBeanResult) throws com.atlassian.confluence.json.parser.JSONException
    {
        // check can create issue with the recieved issue id
        assertNotNull(jiraIssueBeanResult.getId());
        WebResource webResource = RestHelper.newResource(jiraIssueBeanResult.getSelf(), User.ADMIN);
        JSONObject jiraIssueResponse = RestHelper.getJsonResponse(webResource);

        JSONObject jiraIssueFields = (JSONObject) jiraIssueResponse.get("fields");
        // check issue information: summary, description, projectId, issueTypeId
        assertEquals(jiraIssueBeanResult.getSummary(), jiraIssueFields.getString("summary"));
        assertEquals(jiraIssueBeanResult.getDescription(), jiraIssueFields.getString("description"));

        JSONObject project = (JSONObject) jiraIssueFields.get("project");
        assertEquals(jiraIssueBeanResult.getProjectId(), project.getString("id"));

        JSONObject issueType = (JSONObject) jiraIssueFields.get("issuetype");
        assertEquals(jiraIssueBeanResult.getIssueTypeId(), issueType.getString("id"));
    }

    /**
     * Prepare the data of list jira issue for call create issue from confluence
     * 
     * @return List<JiraIssueBean> list of jira issue for call create issue
     */
    private List<JiraIssueBean> createJiraIssueBeans()
    {
        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();
        // get projectId, issueTypeId on dialog
        client.select("css=select.project-select", "index=1");
        client.waitForAjaxWithJquery();
        String projectId = client.getSelectedValue("css=select.project-select");
        String issueTypeId = client.getSelectedValue("css=select.type-select");

        List<JiraIssueBean> jiraIssueBeans = new ArrayList<JiraIssueBean>();
        JiraIssueBean issue1 = new JiraIssueBean(projectId, issueTypeId, "Summary 1", "Description 1");
        jiraIssueBeans.add(issue1);

        JiraIssueBean issue2 = new JiraIssueBean(projectId, issueTypeId, "Summary 2", "Description 2");
        jiraIssueBeans.add(issue2);
        return jiraIssueBeans;
    }

    /**
     * Create JSON string for the list of JiraIssueBean for call Confluence
     * create issues rest
     * 
     * @param jiraIssueBeans
     * @return
     */
    private String createJiraIssuesJsonPayload(List<JiraIssueBean> jiraIssueBeans)
    {
        List<String> issueBeanJsons = new ArrayList<String>();
        for (JiraIssueBean jiraIssueBean : jiraIssueBeans)
        {
            String jiraIssueJson = createJsonStringForJiraIssueBean(jiraIssueBean);
            issueBeanJsons.add(jiraIssueJson);
        }

        return issueBeanJsons.toString();
    }

    /**
     * Create JSON string for call Confluence create issues rest
     * 
     * @param jiraIssueBean
     * @return json string
     */
    private String createJsonStringForJiraIssueBean(JiraIssueBean jiraIssueBean)
    {
        JsonObject issue = new JsonObject();
        issue.setProperty("projectId", jiraIssueBean.getProjectId());
        issue.setProperty("issueTypeId", jiraIssueBean.getIssueTypeId());
        issue.setProperty("summary", jiraIssueBean.getSummary());
        issue.setProperty("description", jiraIssueBean.getDescription());
        return issue.serialize();
    }
}