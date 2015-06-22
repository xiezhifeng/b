package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import org.junit.Ignore;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class TestDefaultJiraIssuesUrlManager extends TestCase
{
    @Mock
    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private DefaultJiraIssuesUrlManager defaultJiraIssuesUrlManager;

    private String url;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        defaultJiraIssuesUrlManager = new DefaultJiraIssuesUrlManager();
        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000";
    }

    public void testGetRequestUrlRemovesQueryString()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml",
                defaultJiraIssuesUrlManager.getRequestUrl(url)
        );
    }

    public void testGetRequestUrlReturnsEmptyStringIfUrlStartsWithQuestionMark()
    {
        assertEquals(
                "",
                defaultJiraIssuesUrlManager.getRequestUrl("?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000")
        );
    }

    public void testGetRequestUrlReturnsUrlUnmodifiedIfItDoesNotContainQuestionMark()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml",
                defaultJiraIssuesUrlManager.getRequestUrl("http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml")
        );
    }

    public void testBogusParameterAddedToXmlUrlForInputUrlsThatDoNotContainQuestionMarks()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/10594/SearchRequest-10594.xml?1=1",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/10594/SearchRequest-10594.xml",
                        null,
                        null,
                        null
                )
        );
    }

    public void testBuildXmlUrlWithTempMaxParameterGetsReadjustedAccordingToResultsPerPage()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        null,
                        null
                )
        );
    }

    public void testBuildXmlUrlWithoutTempMaxParameterGetsOneAppendedAccordingToResultsPerPage()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/10594/SearchRequest-10594.xml?tempMax=10",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/10594/SearchRequest-10594.xml",
                        "10",
                        null,
                        null
                )
        );
    }

    public void testBuildXmlUrlPageParameterAddedIfSpecificPageRequested()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        null,
                        null
                )
        );
    }

    public void testBuildXmlUrlSortByKeyParameterGetsRewrittenAsIssueKey()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0&sorter/field=issuekey",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        "key",
                        null
                )
        );
    }

    public void testBuildXmlUrlSortByTypeParameterGetsRewrittenAsIssueType()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0&sorter/field=issuetype",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        "type",
                        null
                )
        );
    }

    public void testBuildXmlUrlSortByCustomField()
    {
        Map<String, String> columnMap = new HashMap<String, String>();

        columnMap.put("Resolution Date", "customfield_10000");

        when(jiraIssuesColumnManager.getColumnMap(Mockito.anyString())).thenReturn(columnMap);

        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0&sorter/field=customfield_10000",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        "Resolution Date",
                        null
                )
        );
    }

    public void testBuildXmlUrlSortByBuiltInField()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0&sorter/field=summary",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        "summary",
                        null
                )
        );
    }

    public void testBuildXmlUrlWithSortOrder()
    {
        assertEquals(
                "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10&pager/start=0&sorter/field=summary&sorter/order=ASC",
                defaultJiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(
                        url,
                        "10",
                        "1",
                        "summary",
                        "asc"
                )
        );
    }

    private class DefaultJiraIssuesUrlManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesUrlManager
    {
        private DefaultJiraIssuesUrlManager()
        {
            super(jiraIssuesColumnManager);
        }
    }
}
