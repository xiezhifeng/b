package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.macro.ImagePlaceholder;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMacroPlaceHolder extends TestCase {

    @Mock private ApplicationLinkService appLinkService;

    @Mock private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private CacheManager cacheManager;

    @Mock private JiraIssuesManager jiraIssuesManager;

    @Mock private FlexigridResponseGenerator flexigridResponseGenerator;

    private JiraIssuesMacro jiraIssuesMacro;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        jiraIssuesMacro = new JiraIssuesMacro();
        jiraIssuesMacro.setApplicationLinkService(appLinkService);
        jiraIssuesMacro.setJiraIssuesUrlManager(jiraIssuesUrlManager);
        jiraIssuesMacro.setJiraIssuesManager(jiraIssuesManager);
        jiraIssuesMacro.setCacheManager(cacheManager);
        jiraIssuesMacro.setJiraIssuesResponseGenerator(flexigridResponseGenerator);
    }

    public void testGenerateImagePlaceholderWithCount() throws Exception {
        Map<String,String> stringStringMap = new HashMap<String, String>();
        stringStringMap.put("count", "true");
        stringStringMap.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");
        stringStringMap.put("jqlQuery", "project=demo");
        URI uri = new URI("localhost:1990/jira");
        String url = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=";

        ApplicationLink applicationLink = mock(ApplicationLink.class);
        when(applicationLink.getDisplayUrl()).thenReturn(uri);
        url = applicationLink.getDisplayUrl() + url + URLEncoder.encode(stringStringMap.get("jqlQuery"), "UTF-8") + "&tempMax=0";
        when(appLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);
        when(jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, "10", null, null)).thenReturn("jiraIssueXmlUrlWithoutPaginationParam");
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        JiraIssuesManager.Channel channel = mock(JiraIssuesManager.Channel.class);
        when(jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), applicationLink, false)).thenReturn(channel);
        when(flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true)).thenReturn("5");

        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(stringStringMap, null);
        assertEquals(defaultImagePlaceholder.getUrl(), "/plugins/servlet/count-image-generator?totalIssues=5");
    }

    public void testGenerateImagePlaceholderWithNoCount() {
        Map<String,String> stringStringMap = new HashMap<String, String>();
        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(stringStringMap, null);
        assertEquals(defaultImagePlaceholder, null);
    }
}
