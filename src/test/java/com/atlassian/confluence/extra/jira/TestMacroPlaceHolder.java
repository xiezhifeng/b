package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.macro.ImagePlaceholder;

public class TestMacroPlaceHolder extends TestCase
{

    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";
    
    @Mock
    private ApplicationLinkService appLinkService;

    @Mock
    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private JiraIssuesManager jiraIssuesManager;

    @Mock
    private FlexigridResponseGenerator flexigridResponseGenerator;

    private JiraIssuesMacro jiraIssuesMacro;

    private Map<String, String> parameters;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        jiraIssuesMacro = new JiraIssuesMacro();
        parameters = new HashMap<String, String>();

    }

    public void testGenerateImagePlaceholderWithCount() throws Exception
    {
        parameters.put("count", "true");
        parameters.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");
        parameters.put("jqlQuery", "project=demo");

        jiraIssuesMacro.setApplicationLinkService(appLinkService);
        jiraIssuesMacro.setJiraIssuesUrlManager(jiraIssuesUrlManager);
        jiraIssuesMacro.setJiraIssuesManager(jiraIssuesManager);
        jiraIssuesMacro.setCacheManager(cacheManager);
        jiraIssuesMacro.setJiraIssuesResponseGenerator(flexigridResponseGenerator);

        URI uri = new URI("localhost:1990/jira");
        String url = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=";

        ApplicationLink applicationLink = mock(ApplicationLink.class);
        when(applicationLink.getDisplayUrl()).thenReturn(uri);
        url = applicationLink.getDisplayUrl() + url + URLEncoder.encode(parameters.get("jqlQuery"), "UTF-8") + "&tempMax=0";
        when(appLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);
        when(jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, "10", null, null)).thenReturn("jiraIssueXmlUrlWithoutPaginationParam");
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        JiraIssuesManager.Channel channel = mock(JiraIssuesManager.Channel.class);
        when(jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), applicationLink, false, false))
                .thenReturn(channel);
        when(flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true)).thenReturn("5");

        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(defaultImagePlaceholder.getUrl(), "/plugins/servlet/count-image-generator?totalIssues=5");
    }

    public void testGenerateImagePlaceholderWithNoCountAndNoJql()
    {
        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(defaultImagePlaceholder, null);
    }

    public void testGetTableImagePlaceholder()
    {
        parameters.put("jqlQuery", "status=open");
        jiraIssuesMacro = new JiraIssuesMacro();
        ImagePlaceholder imagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(imagePlaceholder.getUrl(), JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH);
    }

}
