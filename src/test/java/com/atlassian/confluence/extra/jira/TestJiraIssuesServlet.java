package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaKeys;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.Cache;
import com.atlassian.cache.memory.MemoryCache;
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
import java.text.ParseException;

public class TestJiraIssuesServlet extends TestCase
{
    private Cache cacheOfCaches;

    public void testCache() throws IOException, ParseException
    {
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();
        Mock mockCacheFactory = new Mock(CacheFactory.class);
        mockCacheFactory.matchAndReturn("getCache", new FullConstraintMatcher(C.eq(JiraIssuesServlet.class.getName())), getCache());
        jiraIssuesServlet.setCacheFactory((CacheFactory)mockCacheFactory.proxy());

        Set columns;
        columns = new LinkedHashSet();
        columns.add("test");
        CacheKey key1 = new CacheKey("usesomethingmorerealistic",columns,false,null,false);

        SimpleStringCache subCacheForKey = new CompressingStringCache(new MemoryCache(key1.getPartialUrl()));
        getCache().put(key1, subCacheForKey);

        String resultForKey1 = "key1 data!";
        Integer requestedPageKey = new Integer(1);
        subCacheForKey.put(requestedPageKey,resultForKey1);

        // getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage, boolean showCount, String url)
        String result = jiraIssuesServlet.getResultJson(key1, false, true, 1, false, "shouldn'tbeused");
        assertEquals(resultForKey1,result);

        // trying to get a page from a different set than the one that is cached
        try
        {
            CacheKey key2 = new CacheKey("usesomethingmorerealistic2",columns,false,null,false);
            String result2 = jiraIssuesServlet.getResultJson(key2, false, true, 1, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - didn't find the item and so had to look it up
        }

        // trying to get a different page than the one that is cached, but from the same set
        try
        {
            result = jiraIssuesServlet.getResultJson(key1, false, true, 2, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - didn't find the item and so had to look it up
        }

        SimpleStringCache tempCopyOfCache = subCacheForKey;
        // trying to get a page that is cached, but with cache flushing on
        try
        {
            assertEquals((getCache().get(key1)),subCacheForKey);

            // useCache = false
            result = jiraIssuesServlet.getResultJson(key1, false, false, 1, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - item was in the cache at first but got flushed and so had to look it up

            // make sure page got cleared
            assertEquals(((CompressingStringCache)getCache().get(new Integer(1))),null);
        }

        // put back the original subcache that got flushed and recreated, so can use it again
        getCache().put(key1,tempCopyOfCache);
        // trying to get a page that isn't cached but is in the same set as one that is cached, but with cache flushing on
        try
        {
            assertEquals((getCache().get(key1)),subCacheForKey);

            // useCache = false
            result = jiraIssuesServlet.getResultJson(key1, false, false, 2, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - item wasn't in the cache and so had to look it up

            // make sure page from same set got cleared
            assertEquals(((CompressingStringCache)getCache().get(new Integer(1))),null);
        }
    }

    private Cache getCache()
    {
        if (cacheOfCaches==null)
            cacheOfCaches = new MemoryCache("testCache");
        return cacheOfCaches;
    }

    public void testCreatePartialUrlFromParams()
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

        String url = JiraIssuesServlet.createPartialUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?&pid=10000&tempMax=1&sorter/field=issuekey&sorter/order=DESC", url); // formerly had &pager/start=0 in it, when this method made the whole url and not just partial
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
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsSet, 1, false);
        assertEquals(expectedJson, json);

        // test with showCount=true
        String jsonCount = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsSet, 1, true);
        assertEquals("1", jsonCount);


        // load other (newer) version of issues xml view
        stream = getResourceAsStream("jiraResponseWithTotal.xml");
        document = saxBuilder.build(stream);
        element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        channel = new JiraIssuesServlet.Channel(element, null);

        // test with showCount=false
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsSet, 1, false);
        assertEquals(expectedJsonWithTotal, json);

        // test with showCount=true
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsSet, 1, true);
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