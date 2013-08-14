package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;

public class CreateIssuesTestCase extends AbstractJiraPanelTestCase
{
    public void testCreateIssues() throws HttpException, IOException, JSONException
    {
        String serverId = getServerId();
        List<JiraIssueBean> jiraIssueBeans = createJiraIssueBeans();
        StringRequestEntity requestEntity = createRequestEntity(jiraIssueBeans);

        // build create issue url with authentication params
        String baseUrl = getConfluenceWebTester().getBaseUrl();
        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);
        String createIssueRestUrl = baseUrl + "/rest/jiraanywhere/1.0" + "/jira-issue/create-jira-issues/" + serverId
                + authArgs;

        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(createIssueRestUrl);
        postMethod.setRequestHeader("Accept", "application/json, text/javascript, */*");
        postMethod.setRequestEntity(requestEntity);

        int status = client.executeMethod(postMethod);
        // check response ok
        assertEquals(200, status);

        String responseBody = postMethod.getResponseBodyAsString();
        postMethod.releaseConnection();

        JSONArray jiraIssueJsonResults = new JSONArray(responseBody);
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
     */
    private void validate(JiraIssueBean jiraIssueBeanResult) throws HttpException, IOException, JSONException
    {
        // check can create issue with the recieved issue id
        assertNotNull(jiraIssueBeanResult.getId());

        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod(jiraIssueBeanResult.getSelf());

        getMethod.setRequestHeader("Accept", "application/json, text/javascript, */*");
        int status = client.executeMethod(getMethod);
        // check response ok
        assertEquals(200, status);
        String responseBody = getMethod.getResponseBodyAsString();

        JSONObject jiraIssueResponse = new JSONObject(responseBody);
        JSONObject jiraIssueFields = (JSONObject) jiraIssueResponse.get("fields");
        // check issue information: summary, description, project id, issue type
        // id
        assertEquals(jiraIssueBeanResult.getSummary(), jiraIssueFields.getString("summary"));
        assertEquals(jiraIssueBeanResult.getDescription(), jiraIssueFields.getString("description"));

        JSONObject project = (JSONObject) jiraIssueFields.get("project");
        assertEquals(jiraIssueBeanResult.getProjectId(), project.getString("id"));

        JSONObject issueType = (JSONObject) jiraIssueFields.get("issuetype");
        assertEquals(jiraIssueBeanResult.getIssueTypeId(), issueType.getString("id"));

        getMethod.releaseConnection();
    }

    private String getServerId() throws HttpException, IOException, JSONException
    {
        // create another jira app link for display server select box to get
        // serverId
        String serverName = "JIRA TEST SERVER1";
        String serverUrl = "http://jira.test.com";
        String serverDisplayUrl = "http://jira.test.com";
        addJiraAppLink(serverName, serverUrl, serverDisplayUrl, false);
        client.refresh();
        client.waitForPageToLoad();
        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();
        // get serverId
        String serverId = client.getSelectedValue("css=select.server-select");
        return serverId;
    }

    /**
     * Prepare the data of list jira issue for call create issue from confluence
     * 
     * @return List<JiraIssueBean> list of jira issue for call create issue
     */
    private List<JiraIssueBean> createJiraIssueBeans()
    {
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
     * Create request enity from list of jira issue beans for http client
     */
    private StringRequestEntity createRequestEntity(List<JiraIssueBean> jiraIssueBeans)
            throws UnsupportedEncodingException
    {
        List<String> issueBeanJsons = new ArrayList<String>();
        for (JiraIssueBean jiraIssueBean : jiraIssueBeans)
        {
            String jiraIssueJson = createJsonStringForJiraIssueBean(jiraIssueBean);
            issueBeanJsons.add(jiraIssueJson);
        }

        StringRequestEntity requestEntity = new StringRequestEntity(issueBeanJsons.toString(), "application/json",
                "UTF-8");
        return requestEntity;
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
