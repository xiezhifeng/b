package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.memory.MemoryCache;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class TestJiraIssuesServlet extends TestCase
{
    private Cache cacheOfCaches;

    public void testCache() throws Exception
    {
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();
        Mock mockCacheFactory = new Mock(CacheFactory.class);
        mockCacheFactory.matchAndReturn("getCache", new FullConstraintMatcher(C.eq(JiraIssuesServlet.class.getName())), getCache());
        jiraIssuesServlet.setCacheFactory((CacheFactory)mockCacheFactory.proxy());

        List columns;
        columns = new ArrayList();
        columns.add("test");
        CacheKey key1 = new CacheKey("usesomethingmorerealistic",columns,false,false);

        SimpleStringCache subCacheForKey = new CompressingStringCache(new HashMap());
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
            CacheKey key2 = new CacheKey("usesomethingmorerealistic2",columns,false,false);
            jiraIssuesServlet.getResultJson(key2, false, true, 1, false, "badhost");
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
            jiraIssuesServlet.getResultJson(key1, false, true, 2, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - didn't find the item and so had to look it up
        }

        // trying to get a page that is cached, but with cache flushing on
        try
        {
            assertEquals((getCache().get(key1)),subCacheForKey);

            // useCache = false
            jiraIssuesServlet.getResultJson(key1, false, false, 1, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - item was in the cache at first but got flushed and so had to look it up

            // make sure page got cleared
            assertEquals(getCache().get(new Integer(1)),null);
        }

        // put back the original subcache that got flushed and recreated, so can use it again
        getCache().put(key1,subCacheForKey);
        // trying to get a page that isn't cached but is in the same set as one that is cached, but with cache flushing on
        try
        {
            assertEquals((getCache().get(key1)),subCacheForKey);

            // useCache = false
            jiraIssuesServlet.getResultJson(key1, false, false, 2, false, "badhost");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            // this exception is okay because I didn't set up this part to work. getting to this point means that the cache part that happens first went how it should
            // - item wasn't in the cache and so had to look it up

            // make sure page from same set got cleared
            assertEquals(getCache().get(new Integer(1)),null);
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
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();

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

        String url = jiraIssuesServlet.createPartialUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&tempMax=1&sorter/field=issuekey&sorter/order=DESC", url); // formerly had &pager/start=0 in it, when this method made the whole url and not just partial

        // testing custom field name to id matching for sortfield
        Mock mockBandanaManager = new Mock(BandanaManager.class);
        JiraIssuesUtils jiraIssuesUtils = new JiraIssuesUtils();
        Map customFields = new HashMap();
        customFields.put("Labels","customfield_10490"); // map field name->id
        mockBandanaManager.matchAndReturn("getValue", new FullConstraintMatcher(C.IS_NOT_NULL, C.eq(JiraIssuesUtils.BANDANA_CUSTOM_FIELDS_PREFIX + "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml")), customFields);
        jiraIssuesUtils.setBandanaManager((BandanaManager)mockBandanaManager.proxy());

        params.put("columns",new String[]{"type","key","summary","reporter","status","Labels"});
        params.put("sortname",new String[]{"Labels"});
        jiraIssuesServlet.setJiraIssuesUtils(jiraIssuesUtils);
        url = jiraIssuesServlet.createPartialUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&tempMax=1&sorter/field=customfield_10490&sorter/order=DESC", url);
    }

    public void testCreatePartialUrlFromParamsUrlEmpty()
    {
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();

        Map params = new HashMap();
        params.put("url",null);

        try
        {
            String url = jiraIssuesServlet.createPartialUrlFromParams(params);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        params.put("url",new String[]{""});

        try
        {
            String url = jiraIssuesServlet.createPartialUrlFromParams(params);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testConvertJiraResponseToJson() throws Exception
    {
        Mock mockBandanaManager = new Mock(BandanaManager.class);

        JiraIssuesUtils jiraIssuesUtils = new JiraIssuesUtils();
        Mock mockTransactionManager = new Mock(PlatformTransactionManager.class);
        Mock mockTransactionStatus = new Mock(TransactionStatus.class);
        
        mockTransactionManager.matchAndReturn("getTransaction", C.ANY_ARGS, mockTransactionStatus.proxy());
        mockTransactionManager.matchAndReturn("commit", C.ANY_ARGS, null);
        jiraIssuesUtils.setTransactionManager((PlatformTransactionManager)mockTransactionManager.proxy());

        mockBandanaManager.matchAndReturn("setValue", new FullConstraintMatcher(C.IS_NOT_NULL, C.IS_NOT_NULL, C.IS_NOT_NULL), null);
        jiraIssuesUtils.setBandanaManager((BandanaManager)mockBandanaManager.proxy());

        List columnsList = new ArrayList();
        columnsList.add("type");
        columnsList.add("key");
        columnsList.add("summary");
        columnsList.add("reporter");
        columnsList.add("status");

        SAXBuilder saxBuilder = new SAXBuilder(JiraIssuesUtils.SAX_PARSER_CLASS);
        InputStream stream = getResourceAsStream("jiraResponse.xml");

        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);
        JiraIssuesServlet jiraIssuesServlet = new JiraIssuesServlet();
        JiraIconMappingManager jiraIconMappingManager = new JiraIconMappingManager();

        Map jiraIconMap = new HashMap();
        jiraIconMap.put("Task", "http://localhost:8080/images/icons/task.gif");
        mockBandanaManager.matchAndReturn("getValue", new FullConstraintMatcher(C.IS_NOT_NULL, C.eq(ConfluenceBandanaKeys.JIRA_ICON_MAPPINGS)), jiraIconMap);
        jiraIconMappingManager.setBandanaManager((BandanaManager)mockBandanaManager.proxy());
        jiraIssuesServlet.setJiraIconMappingManager(jiraIconMappingManager);
        jiraIssuesServlet.setJiraIssuesUtils(jiraIssuesUtils);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJson, json);

        // test with showCount=true
        String jsonCount = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, true, "fakeurl");
        assertEquals("1", jsonCount);


        // load other (newer) version of issues xml view
        stream = getResourceAsStream("jiraResponseWithTotal.xml");
        document = saxBuilder.build(stream);
        element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithTotal, json);

        // test with showCount=true
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, true, "fakeurl");
        assertEquals("3", json);

        // load other (newer) version of issues xml view
        stream = getResourceAsStream("jiraResponseWithApostrophe.xml");
        document = saxBuilder.build(stream);
        element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithApostrophe, json);
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

    String expectedJsonWithApostrophe = "{\n"+
        "page: 1,\n"+
        "total: 1,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'CONF-12242',cell:['<a href=\"http://jira.atlassian.com/browse/CONF-12242\" ><img src=\"null\" alt=\"Bug\"/></a>','<a href=\"http://jira.atlassian.com/browse/CONF-12242\" >CONF-12242</a>','<a href=" +
        "\"http://jira.atlassian.com/browse/CONF-12242\" >Numbered List sub-items render differently in RSS versus browser</a>','David O\\'Flynn [Atlassian]','Open']}\n"+
        "\n"+
        "]}";

}