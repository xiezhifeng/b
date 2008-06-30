package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaKeys;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TestJiraIssuesServlet extends TestCase
{
    public void testCreateUrlFromParams()
    {
        Map params = new HashMap();
        params.put("useTrustedConnection",new String[]{"true"});
        params.put("sortorder",new String[]{"desc"});
        params.put("query",new String[]{""});
        params.put("page",new String[]{"1"});
        params.put("url",new String[]{"http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?&pid=10000"});
        params.put("qtype",new String[]{""});
        params.put("columns",new String[]{"type","key","summary","reporter","status"});
        params.put("qtype",new String[]{""});
        params.put("sortname",new String[]{"key"});
        params.put("rp",new String[]{"1"});

        String url = JiraIssuesServlet.createUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?&pid=10000&pager/start=0&tempMax=1&sorter/field=issuekey&sorter/order=DESC", url);
    }

    public void testConvertJiraResponseToJson() throws Exception
    {
        Set columnsSet = new LinkedHashSet();
        columnsSet.add("type");
        columnsSet.add("key");
        columnsSet.add("summary");
        columnsSet.add("reporter");
        columnsSet.add("status");

        SAXBuilder saxBuilder = new SAXBuilder(JiraIssuesServlet.SAX_PARSER_CLASS);
        InputStream stream = getResourceAsStream("jiraResponse.xml");

        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesServlet.Channel channel = new JiraIssuesServlet.Channel(element, null);
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();
        JiraIconMappingManager jiraIconMappingManager = new JiraIconMappingManager();

        Map jiraIconMap = new HashMap();
        jiraIconMap.put("Task", "http://localhost:8080/images/icons/task.gif");
        Mock mockBandanaManager = new Mock(BandanaManager.class);
        mockBandanaManager.matchAndReturn("getValue", new FullConstraintMatcher(C.IS_NOT_NULL, C.eq(ConfluenceBandanaKeys.JIRA_ICON_MAPPINGS)), jiraIconMap);
        jiraIconMappingManager.setBandanaManager((BandanaManager)mockBandanaManager.proxy());
        jiraIssuesServlet.setJiraIconMappingManager(jiraIconMappingManager);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToJson(channel, columnsSet, 1, false);
        assertEquals(expectedJson, json);

        // test with showCount=true
        String jsonCount = jiraIssuesServlet.jiraResponseToJson(channel, columnsSet, 1, true);
        assertEquals("1", jsonCount);


        // load other (newer) version of issues xml view
        stream = getResourceAsStream("jiraResponseWithTotal.xml");
        document = saxBuilder.build(stream);
        element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        channel = new JiraIssuesServlet.Channel(element, null);

        // test with showCount=false
        json = jiraIssuesServlet.jiraResponseToJson(channel, columnsSet, 1, false);
        assertEquals(expectedJsonWithTotal, json);

        // test with showCount=true
        json = jiraIssuesServlet.jiraResponseToJson(channel, columnsSet, 1, true);
        assertEquals("3", json);
    }

    private InputStream getResourceAsStream(String name) throws IOException
    {
        URL url = getClass().getClassLoader().getResource(name);
        return url.openStream();
    }


    String expectedJson = "{\n"+
        "page: 1,\n"+
        "total: 1,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','Closed']}\n"+
        "\n"+
        "]}";

    String expectedJsonWithTotal = "{\n"+
        "page: 1,\n"+
        "total: 3,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','Closed']}\n"+
        "\n"+
        "]}";

}