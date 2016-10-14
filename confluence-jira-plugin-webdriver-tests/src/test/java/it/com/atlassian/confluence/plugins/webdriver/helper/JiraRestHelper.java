package it.com.atlassian.confluence.plugins.webdriver.helper;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.RestHelper;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.json.JsonBoolean;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapService;
import com.atlassian.connector.commons.jira.soap.axis.JiraSoapServiceServiceLocator;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraRestHelper
{
    private static final String ISSUE_ENDPOINT = AbstractJiraTest.JIRA_BASE_URL + "/rest/api/2/issue";
    private static final String FILTER_ENDPOINT = AbstractJiraTest.JIRA_BASE_URL + "/rest/api/2/filter";

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

    // Temporary method used to initialise the JIRA SOAP variables if we are testing against OD instances
    // This can be removed when all the SOAP service calls are replaced with REST calls
    public static void initJiraSoapServices() throws Exception
    {
        // TODO Update to use JIRA's REST API once it supports project creation
        // Create JiraSoapService (only used for project creation)
        // NOTE: JIRA's SOAP and XML-RPC API has already been deprecated as of 6.0 and will be removed in 7.0 but the REST
        // API which replaces SOAP currently does not provide the capability of creating projects
        JiraSoapServiceServiceLocator soapServiceLocator = new JiraSoapServiceServiceLocator();
        soapServiceLocator.setJirasoapserviceV2EndpointAddress(AbstractJiraTest.JIRA_BASE_URL + "/rpc/soap/jirasoapservice-v2?wsdl");
        jiraSoapService = soapServiceLocator.getJirasoapserviceV2();
        jiraSoapToken = jiraSoapService.login(User.ADMIN.getUsername(), User.ADMIN.getPassword());
    }

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

    public static String createJiraFilter(String name, String jql, String description, CloseableHttpClient httpClient)
    {
        JsonObject filter = new JsonObject()
                .setProperty("name", name)
                .setProperty("description", description)
                .setProperty("jql", jql)
                .setProperty("favourite", new JsonBoolean(true));

        try
        {
            HttpPost method = new HttpPost(FILTER_ENDPOINT + "?" + getAuthenticationParams());
            method.setHeader("Accept", "application/json");

            method.setEntity(new StringEntity(filter.serialize(), ContentType.APPLICATION_JSON));

            try(CloseableHttpResponse response = httpClient.execute(method)){
                String responseBodyString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                JSONObject responseBody = new JSONObject(responseBodyString);
                return responseBody.getString("id");
            }
        }
        catch (Exception e)
        {
            log.error("Error creating JIRA filter", e);
            return null;
        }
    }

    public static int deleteJiraFilter(String filterId, CloseableHttpClient httpClient)
    {
        int status = 0;

        try
        {
            HttpDelete method = new HttpDelete(FILTER_ENDPOINT + "/" + filterId + "?" + getAuthenticationParams());
            try(CloseableHttpResponse response = httpClient.execute(method)){
                status = response.getStatusLine().getStatusCode();
            }
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
