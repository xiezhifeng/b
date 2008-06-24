package com.atlassian.confluence.extra.jira;

import com.atlassian.renderer.RenderContext;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TestJiraIssuesMacro extends TestCase
{
    // TODO: make this (just created) test work and add more varieties
//    public void testCreateContextMapForTemplate() throws Exception
//    {
//        Map params = new HashMap();
//        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC");
//        params.put("columns", "type, summary");
//
//        Map expectedContextMap = new HashMap();
//        expectedContextMap.put("useTrustedConnection", new Boolean(false));
//        expectedContextMap.put("startOn", new Integer(0));
//        expectedContextMap.put("clickableUrl", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC");
//        expectedContextMap.put("showCount", new Boolean(false));
//        expectedContextMap.put("resultsPerPage", new Integer(Integer.MAX_VALUE));
//        expectedContextMap.put("macroId", "jiraissues_0");
//        expectedContextMap.put("url", "http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000");
//        expectedContextMap.put("sortOrder", "desc");
//        expectedContextMap.put("sortField", "issuekey");
//        Set cols = new LinkedHashSet();
//        cols.add("type");
//        cols.add("summary");
//        expectedContextMap.put("columns", cols);
//        expectedContextMap.put("useCache", new Boolean(true));
//
//        JiraIssuesMacro jiraIssuesMacro = new JiraIssuesMacro();
//        Map contextMap =  new HashMap();
//        jiraIssuesMacro.createContextMapFromParams(params,new RenderContext(), contextMap); // (RenderContext)mockRenderContext.proxy()
//        assertEquals(expectedContextMap, contextMap);
//    }

    public void testFilterOutParam()
    {
        String expectedUrl = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC";
        String filter = "tempMax=";
        String value;

        StringBuffer urlWithParamAtEnd = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC&tempMax=259");
        value = JiraIssuesMacro.filterOutParam(urlWithParamAtEnd, filter);
        assertEquals("259", value);
        assertEquals(expectedUrl, urlWithParamAtEnd.toString());

        StringBuffer urlWithParamAtBeginning = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?tempMax=1&pid=10000&sorter/field=issuekey&sorter/order=DESC");
        value = JiraIssuesMacro.filterOutParam(urlWithParamAtBeginning, filter);
        assertEquals("1", value);
        assertEquals(expectedUrl, urlWithParamAtBeginning.toString());

        StringBuffer urlWithParamInMiddle = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&tempMax=30&sorter/order=DESC");
        value = JiraIssuesMacro.filterOutParam(urlWithParamInMiddle, filter);
        assertEquals("30", value);
        assertEquals(expectedUrl, urlWithParamInMiddle.toString());

    }
}