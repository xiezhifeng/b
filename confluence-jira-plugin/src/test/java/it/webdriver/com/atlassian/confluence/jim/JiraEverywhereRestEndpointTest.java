package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class JiraEverywhereRestEndpointTest extends AbstractJIMTest
{

    @Test
    public void testCreateIssues() throws ClientHandlerException, UniformInterfaceException, JSONException, JsonParseException, JsonMappingException, IOException
    {
        String serverId = getPrimaryApplinkId();

        assertTrue(StringUtils.isNotEmpty(serverId));

        List<JiraIssueBean> jiraIssueBeans = createJiraIssueBeans();
        String jiraIssueBeansJsonPayload = createJiraIssuesJsonPayload(jiraIssueBeans);

        String createIssueRestUrl = rpc.getBaseUrl() + "/rest/jiraanywhere/1.0" + "/jira-issue/create-jira-issues/" + serverId;

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
    private void validate(JiraIssueBean jiraIssueBeanResult)
    {
        // check can create issue with the recieved issue id
        assertTrue(jiraIssueBeanResult.getId() != null);
        WebResource webResource = RestHelper.newResource(jiraIssueBeanResult.getSelf(), User.ADMIN);
        JsonNode jiraIssueResponse = null;
        try
        {
            jiraIssueResponse = RestHelper.fetchJsonResponse(webResource);
        } catch (IOException e)
        {
            fail("couldn't fetch issue from web resource : " + e.getMessage());
        }

        JsonNode jiraIssueFields = jiraIssueResponse.get("fields");
        // check issue information: summary, description, projectId, issueTypeId
        assertTrue(jiraIssueBeanResult.getSummary().equals(jiraIssueFields.get("summary").getTextValue()));
        assertTrue(jiraIssueBeanResult.getDescription().equals(jiraIssueFields.get("description").getTextValue()));

        JsonNode project = jiraIssueFields.get("project");
        assertTrue(jiraIssueBeanResult.getProjectId().equals(project.get("id").getTextValue()));

        JsonNode issueType = jiraIssueFields.get("issuetype");
        assertTrue(jiraIssueBeanResult.getIssueTypeId().equals(issueType.get("id").getTextValue()));
    }

    /**
     * Prepare the data of list jira issue for call create issue from confluence
     * 
     * @return List<JiraIssueBean> list of jira issue for call create issue
     */
    private List<JiraIssueBean> createJiraIssueBeans()
    {
        String projectId = "10020"; // T2T project
        String issueTypeId = "1"; // Bug issue type 

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
