package it.webdriver.com.atlassian.confluence;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Assert;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;

public class JiraTestUtil
{
    private String jiraBaseUrl;

    public JiraTestUtil(String jiraBaseUrl)
    {
        this.jiraBaseUrl = jiraBaseUrl;
    }

    public String createNewIssue(String projectId, String issueTypeId, String summary)
    {
        HttpClient client = new HttpClient();
        try
        {
            doJiraWebSudo(client);
            JiraIssueBean issueBean = new JiraIssueBean(projectId, issueTypeId, summary, "");
            return doCreateJiraIssue(client, getAuthQueryString(), issueBean);
        }
        catch (HttpException e)
        {
            Assert.fail("Could not authenticate into JIRA.");
        }
        catch (IOException e)
        {
            Assert.fail("Could not authenticate into JIRA.");
        }

        return "";
    }

    public void deleteIssue(String id)
    {
        HttpClient client = new HttpClient();
        try
        {
            doJiraWebSudo(client);
            final DeleteMethod m = new DeleteMethod(jiraBaseUrl + "/rest/api/2/issue/" + id + getAuthQueryString());
            final int status = client.executeMethod(m);
            Assert.assertTrue(status == HttpStatus.SC_NO_CONTENT);
        }
        catch (HttpException e)
        {
            Assert.fail("Could not authenticate into JIRA.");
        }
        catch (IOException e)
        {
            Assert.fail("Could not authenticate into JIRA.");
        }
    }

    private void doJiraWebSudo(HttpClient client) throws HttpException, IOException
    {
        final PostMethod l = new PostMethod(jiraBaseUrl + "/secure/admin/WebSudoAuthenticate.jspa");
        l.addParameter("webSudoPassword", User.ADMIN.getPassword());
        l.addParameter("authenticate", "Confirm");
        final int status = client.executeMethod(l);
        Assert.assertTrue(status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK);
    }

    private String doCreateJiraIssue(HttpClient client, String authArgs, JiraIssueBean issueBean) throws HttpException, IOException
    {
        final PostMethod m = new PostMethod(jiraBaseUrl + "/rest/api/2/issue/" + authArgs);
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = JiraUtil.createJsonStringForJiraIssueBean(issueBean);
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody, "application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_CREATED, status);

        String response = m.getResponseBodyAsString();
        BasicJiraIssueBean createdIssue = JiraUtil.createBasicJiraIssueBeanFromResponse(response);
        return createdIssue.getId();
    }

    private String getAuthQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }

}
