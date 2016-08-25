package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ReturningResponseHandler;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.Assert;
import junit.framework.TestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestDefaultJiraIssuesManager extends TestCase
{
    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private TrustedTokenFactory trustedTokenFactory;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    @Mock private HttpRetrievalService httpRetrievalService;

    @Mock private ReadOnlyApplicationLinkService appLinkService;

    @Mock private HttpResponse httpResponse;

    private String url;

    private String urlWithoutQueryString;

    private DefaultJiraIssuesManager defaultJiraIssuesManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);

        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000";
        urlWithoutQueryString = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";

        defaultJiraIssuesManager = new DefaultJiraIssuesManager();
    }

    public void testColumnsForURL()
    {
        ArrayList<String> columns = Lists.newArrayList("Summary", "Type");
        when(jiraIssuesColumnManager.getCanonicalFormOfBuiltInField("Summary")).thenReturn("summary");
        when(jiraIssuesColumnManager.getCanonicalFormOfBuiltInField("Type")).thenReturn("type");
        when(jiraIssuesColumnManager.isColumnBuiltIn("type")).thenReturn(false);
        when(jiraIssuesColumnManager.isColumnBuiltIn("summary")).thenReturn(false);

        String fieldRestrictedUrl = defaultJiraIssuesManager.getFieldRestrictedUrl(columns, "http://test.com?nomatter");
        assertTrue(fieldRestrictedUrl.contains("field=summary"));
        assertFalse(fieldRestrictedUrl.contains("field=Summary"));
        assertTrue(fieldRestrictedUrl.contains("field=type"));
        assertFalse(fieldRestrictedUrl.contains("field=Type"));
    }

    public void testGetColumnMapFromJiraIssuesColumnManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();

        when(jiraIssuesColumnManager.getColumnMap(urlWithoutQueryString)).thenReturn(columnMap);
        assertSame(columnMap, defaultJiraIssuesManager.getColumnMap(url));
    }

    public void testSetColumnMapToJiraIssuesColumnManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();


        defaultJiraIssuesManager.setColumnMap(url, columnMap);
        verify(jiraIssuesColumnManager).setColumnMap(urlWithoutQueryString, columnMap);
    }

     /**
     * Tests that MalforedRequestException is thrown by {@link DefaultJiraIssuesManager#retrieveXMLAsChannel}
      * @throws ResponseException
     * @throws CredentialsRequiredException
     */
    public void testMalformedRequestExceptionThrown() throws IOException, CredentialsRequiredException, ResponseException
    {
        when(httpRetrievalService.get((HttpRequest) any())).thenReturn(httpResponse);
        when(httpResponse.isFailed()).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        try
        {
            defaultJiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), null, true, false);
            fail("Expected a MalformedRequestException");
        }
        catch (MalformedRequestException mre)
        { // ExpectedException
        }
    }

    /**
     * Tests that Authenticationexception is thrown by {@link DefaultJiraIssuesManager#retrieveXMLAsChannel}
     * @throws ResponseException
     * @throws CredentialsRequiredException
     */
    public void testAuthenticationExceptionThrown() throws IOException, CredentialsRequiredException, ResponseException
    {
        when(httpRetrievalService.get((HttpRequest) any())).thenReturn(httpResponse);
        when(httpResponse.isFailed()).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);
        try
        {
            defaultJiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), null, true, false);
            fail("Expected an AuthenticationException");
        }
        catch (AuthenticationException mre)
        { // ExpectedException
        }
    }

    @Test
    public void testCreateIssuesInSingle() throws CredentialsRequiredException, ResponseException
    {
        ReadOnlyApplicationLink applicationLink = createMockApplicationLink(createJsonResultSingle("1", "TP-1", "http://jira.com/TP-1"));

        List<JiraIssueBean> jiraIssueBeansIn = createJiraIssueBean(1);
        Assert.assertNull(jiraIssueBeansIn.get(0).getId());

        List<JiraIssueBean> jiraIssueBeansOut = defaultJiraIssuesManager.createIssues(jiraIssueBeansIn, applicationLink);

        Assert.assertEquals(1, jiraIssueBeansOut.size());
        Assert.assertEquals("1", jiraIssueBeansOut.get(0).getId());
        Assert.assertEquals("Summary0", jiraIssueBeansOut.get(0).getSummary());
    }

    @Test
    public void testCreateIssuesInBatch() throws CredentialsRequiredException, ResponseException
    {
        ReadOnlyApplicationLink applicationLink = createMockApplicationLink(
                createJsonResultBatch(createJsonResultSingle("1", "2", "3"),
                                      createJsonResultSingle("11", "22", "33")));

        List<JiraIssueBean> jiraIssueBeansIn = createJiraIssueBean(2);
        Assert.assertNull(jiraIssueBeansIn.get(0).getId());
        Assert.assertNull(jiraIssueBeansIn.get(1).getId());

        List<JiraIssueBean> jiraIssueBeansOut = defaultJiraIssuesManager.createIssues(jiraIssueBeansIn, applicationLink);

        Assert.assertEquals(2, jiraIssueBeansOut.size());
        Assert.assertEquals("1", jiraIssueBeansOut.get(0).getId());
        Assert.assertEquals("Summary0", jiraIssueBeansOut.get(0).getSummary());
        Assert.assertEquals("11", jiraIssueBeansOut.get(1).getId());
        Assert.assertEquals("Summary1", jiraIssueBeansOut.get(1).getSummary());
    }

    @Test
    public void testCreateIssuesWithJiraVersionBefore6() throws CredentialsRequiredException, ResponseException
    {
        ReadOnlyApplicationLink applicationLink = createMockApplicationLink(
                createJsonResultSingle("1", "TP-1", "http://jira.com/TP-1"),
                createJsonResultSingle("11", "TP-1", "http://jira.com/TP-1"));

        List<JiraIssueBean> jiraIssueBeansIn = createJiraIssueBean(2);
        Assert.assertNull(jiraIssueBeansIn.get(0).getId());
        Assert.assertNull(jiraIssueBeansIn.get(1).getId());

        DefaultJiraIssueManagerBeforeV6 jiraBefore6 = new DefaultJiraIssueManagerBeforeV6();
        List<JiraIssueBean> jiraIssueBeansOut = jiraBefore6.createIssues(jiraIssueBeansIn, applicationLink);

        Assert.assertEquals(2, jiraIssueBeansOut.size());
        Assert.assertEquals("1", jiraIssueBeansOut.get(0).getId());
        Assert.assertEquals("Summary0", jiraIssueBeansOut.get(0).getSummary());
        Assert.assertEquals("11", jiraIssueBeansOut.get(1).getId());
        Assert.assertEquals("Summary1", jiraIssueBeansOut.get(1).getSummary());
    }

    private String createJsonResultSingle(String id, String key, String self)
    {
        BasicJiraIssueBean basicJiraIssueBean = new BasicJiraIssueBean();
        basicJiraIssueBean.setId(id);
        basicJiraIssueBean.setKey(key);
        basicJiraIssueBean.setSelf(self);
        return new Gson().toJson(basicJiraIssueBean);
    }
    private String createJsonResultBatch(String... issues)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("{").append("\"issues\":").append("[");
        sb.append(StringUtils.join(issues, ","));
        sb.append("]");
        sb.append(",\"errors\" :[]");
        sb.append("}");
        return sb.toString();
    }

    private List<JiraIssueBean> createJiraIssueBean(int size)
    {
        List<JiraIssueBean> jiraIssueBeans = Lists.newArrayList();
        for (int i = 0; i < size; i++)
        {
            JiraIssueBean jiraIssueBean = new JiraIssueBean();
            jiraIssueBean.setIssueTypeId("1");
            jiraIssueBean.setProjectId("1000");
            jiraIssueBean.setSummary("Summary" + i);

            jiraIssueBeans.add(jiraIssueBean);
        }
        return jiraIssueBeans;
    }


    private ReadOnlyApplicationLink createMockApplicationLink(String willReturnWhenExecute, String...nextExecutedValues) throws CredentialsRequiredException, ResponseException
    {
        ReadOnlyApplicationLink applicationLink = mock(ReadOnlyApplicationLink.class);
        ApplicationLinkRequestFactory applicationLinkRequestFactory = mock(ApplicationLinkRequestFactory.class);
        ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);

        when(applicationLink.getId()).thenReturn(new ApplicationId(UUID.randomUUID().toString()));
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(applicationLinkRequestFactory);
        when(applicationLinkRequestFactory.createRequest(any(MethodType.POST.getClass()) , anyString())).thenReturn(applicationLinkRequest);
        when(applicationLinkRequest.execute()).thenReturn(willReturnWhenExecute, nextExecutedValues);
        when(applicationLinkRequest.executeAndReturn((ReturningResponseHandler<Response, String>)any()))
            .thenReturn(willReturnWhenExecute, nextExecutedValues);
        return applicationLink;
    }


    private class DefaultJiraIssuesManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesManager
    {
        private DefaultJiraIssuesManager()
        {
            super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService);
        }
        protected Boolean isSupportBatchIssue(ReadOnlyApplicationLink appLink)
        {
            return true;
        }
    }
    private class DefaultJiraIssueManagerBeforeV6 extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesManager
    {
        private DefaultJiraIssueManagerBeforeV6()
        {
            super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService);
        }
        protected Boolean isSupportBatchIssue(ReadOnlyApplicationLink appLink)
        {
            return false;
        }
    }

}
