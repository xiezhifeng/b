package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.memory.MemoryCache;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaKeys;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;

public class TestJiraIssuesServlet extends MockObjectTestCase
{
    private Cache cacheOfCaches;

    private JiraIssuesServlet jiraIssuesServlet;

    private Mock mockCacheFactory;

    private CacheFactory cacheFactory;

    private Mock mockBandanaManager;

    private BandanaManager bandanaManager;

    private Mock mockTransactionManager;

    private PlatformTransactionManager transactionManager;

    private Mock mockTransactionStatus;

    private TransactionStatus transactionStatus;

    private JiraIssuesUtils jiraIssuesUtils;

    private List columnsList = new ArrayList();
    private SAXBuilder saxBuilder = new SAXBuilder(JiraIssuesUtils.SAX_PARSER_CLASS);
    private Map jiraIconMap = new HashMap();

    protected void setUp() throws Exception
    {
        super.setUp();

        mockCacheFactory = new Mock(CacheFactory.class);
        cacheFactory = (CacheFactory) mockCacheFactory.proxy();

        mockBandanaManager = new Mock(BandanaManager.class);
        bandanaManager = (BandanaManager) mockBandanaManager.proxy();

        mockTransactionManager = new Mock(PlatformTransactionManager.class);
        transactionManager = (PlatformTransactionManager) mockTransactionManager.proxy();

        mockTransactionStatus = new Mock(TransactionStatus.class);
        transactionStatus = (TransactionStatus) mockTransactionStatus.proxy();

        jiraIssuesServlet = new JiraIssuesServlet();
        jiraIssuesServlet.setCacheFactory(cacheFactory);

        jiraIssuesUtils = new JiraIssuesUtils();
        jiraIssuesUtils.setBandanaManager(bandanaManager);
        jiraIssuesUtils.setTransactionManager(transactionManager);

        columnsList.add("type");
        columnsList.add("key");
        columnsList.add("summary");
        columnsList.add("reporter");
        columnsList.add("status");

        jiraIconMap.put("Task", "http://localhost:8080/images/icons/task.gif");

        JiraIconMappingManager jiraIconMappingManager = new JiraIconMappingManager();
        jiraIconMappingManager.setBandanaManager((BandanaManager)mockBandanaManager.proxy());
        jiraIssuesUtils.setJiraIconMappingManager(jiraIconMappingManager);
        jiraIssuesServlet.setJiraIssuesUtils(jiraIssuesUtils);
    }

    public void testCache() throws Exception
    {
        setExpectationsForConversion();

        mockCacheFactory.expects(atLeastOnce()).method("getCache").with(eq(JiraIssuesServlet.class.getName())).will(returnValue(getCache()));

        Mock mockHttpRetrievalService = new Mock(HttpRetrievalService.class);
        jiraIssuesUtils.setHttpRetrievalService((HttpRetrievalService) mockHttpRetrievalService.proxy());

        List columns;
        columns = new ArrayList();
        columns.add("key");
        CacheKey key1 = new CacheKey("usesomethingmorerealistic",columns,false,false);

        SimpleStringCache subCacheForKey = new CompressingStringCache(new HashMap());
        getCache().put(key1, subCacheForKey);

        String resultForKey1 = "key1 data!";
        Integer requestedPageKey = new Integer(1);
        subCacheForKey.put(requestedPageKey,resultForKey1);

        // getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage, boolean showCount, String url)
        String result = jiraIssuesServlet.getResultJson(key1, false, true, 1, false, "shouldn'tbeused");
        assertEquals(resultForKey1,result);
        mockHttpRetrievalService.verify(); // should not have been called

        // trying to get a page from a different set than the one that is cached (expect an http request)
        CacheKey key2 = new CacheKey("usesomethingmorerealistic2",columns,false,false);
        expectHttpRequest(mockHttpRetrievalService, "badhost");
        jiraIssuesServlet.getResultJson(key2, false, true, 1, false, "badhost");
        mockHttpRetrievalService.verify();

        // trying to get a different page than the one that is cached, but from the same set (expect an http request)
        expectHttpRequest(mockHttpRetrievalService, "badhost");
        jiraIssuesServlet.getResultJson(key1, false, true, 2, false, "badhost");
        mockHttpRetrievalService.verify();

        // trying to get a page that is cached, but with cache flushing on (expect an http request)
        assertEquals((getCache().get(key1)),subCacheForKey);
        expectHttpRequest(mockHttpRetrievalService, "badhost");
        jiraIssuesServlet.getResultJson(key1, false, false, 1, false, "badhost");
        mockHttpRetrievalService.verify();

        // make sure page got cleared
        assertEquals(getCache().get(new Integer(1)),null);

        // put back the original subcache that got flushed and recreated, so can use it again
        getCache().put(key1,subCacheForKey);
        // trying to get a page that isn't cached but is in the same set as one that is cached, but with cache flushing on
        assertEquals((getCache().get(key1)),subCacheForKey);
        expectHttpRequest(mockHttpRetrievalService, "badhost");
        jiraIssuesServlet.getResultJson(key1, false, false, 2, false, "badhost");
        mockHttpRetrievalService.verify();
        // make sure page from same set got cleared
        assertEquals(getCache().get(new Integer(1)),null);
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

        String url = jiraIssuesServlet.createPartialUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&tempMax=1&sorter/field=issuekey&sorter/order=DESC", url); // formerly had &pager/start=0 in it, when this method made the whole url and not just partial

        // testing custom field name to id matching for sortfield
        Map customFields = new HashMap();
        customFields.put("Labels","customfield_10490"); // map field name->id
        mockBandanaManager.expects(once()).method("getValue").with(
                NOT_NULL,
                eq(JiraIssuesUtils.BANDANA_CUSTOM_FIELDS_PREFIX + DigestUtils.md5Hex("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml"))
        ).will(returnValue(customFields));

        params.put("columns",new String[]{"type","key","summary","reporter","status","Labels"});
        params.put("sortname",new String[]{"Labels"});
        jiraIssuesServlet.setJiraIssuesUtils(jiraIssuesUtils);
        url = jiraIssuesServlet.createPartialUrlFromParams(params);
        assertEquals("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&tempMax=1&sorter/field=customfield_10490&sorter/order=DESC", url);
    }

    public void testCreatePartialUrlFromParamsUrlEmpty()
    {
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

    private void setExpectationsForConversion()
    {
        mockTransactionManager.expects(atLeastOnce()).method("getTransaction").with(ANYTHING).will(returnValue(transactionStatus));
        mockBandanaManager.expects(atLeastOnce()).method("setValue").with(NOT_NULL, NOT_NULL, NOT_NULL);
        mockTransactionManager.expects(atLeastOnce()).method("commit").with(same(transactionStatus));

        mockBandanaManager.expects(atLeastOnce()).method("getValue").with(
                NOT_NULL, eq(JiraIconMappingManager.JIRA_ICON_MAPPINGS)
        ).will(returnValue(jiraIconMap));
    }

    public void testConvertJiraResponseToJson() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponse.xml");

        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJson, json);

        // test with showCount=true
        String jsonCount = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, true, "fakeurl");
        assertEquals("1", jsonCount);
    }

    // load other (newer) version of issues xml view
    public void testConvertJiraResponseToJsonWithTotal() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponseWithTotal.xml");
        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithTotal, json);

        // test with showCount=true
        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, true, "fakeurl");
        assertEquals("3", json);
    }

    // load other (newer) version of issues xml view, with an apostrophe
    public void testConvertJiraResponseToJsonWithApostrophe() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponseWithApostrophe.xml");
        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithApostrophe, json);
    }

    // load other (newer) version of issues xml view, with an ampersand and an oomlaut
    public void testConvertJiraResponseToJsonWithOddCharacters() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponseWithOddCharacters.xml");
        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithOddCharsAndNoMap, json);
        
        // Modify icon map
        jiraIconMap.put("B\u00FCg", "http://localhost:8080/images/icons/improvement.gif");
        jiraIconMap.put("New & Improved", "http://localhost:8080/images/icons/improvement.gif");

        json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonWithOddCharsAndIconMap, json);
    }

    
    // load issues xml view without iconUrls in some cases
    public void testConvertJiraResponseToJsonNoIconUrl() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponseNoIconUrl.xml");
        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonNoIconUrl, json);
    }

    // load issues xml view that contains a javascript alert
    public void testConvertJiraResponseToJsonJsReporter() throws Exception
    {
        setExpectationsForConversion();

        InputStream stream = getResourceAsStream("jiraResponseJsReporter.xml");
        Document document = saxBuilder.build(stream);
        Element element = (Element) XPath.selectSingleNode(document, "/rss//channel");
        JiraIssuesUtils.Channel channel = new JiraIssuesUtils.Channel(element, null);

        columnsList = new ArrayList();
        columnsList.add("reporter");
        // test with showCount=false
        String json = jiraIssuesServlet.jiraResponseToOutputFormat(channel, columnsList, 1, false, "fakeurl");
        assertEquals(expectedJsonJsReporter, json);
    }

    private InputStream getResourceAsStream(String name) throws IOException
    {
        URL url = getClass().getClassLoader().getResource(name);
        return url.openStream();
    }

    private void expectHttpRequest(Mock mockHttpRetrievalService, String url)
    {
        HttpRequest req = new HttpRequest();
        mockHttpRetrievalService.expects(once()).method("getDefaultRequestFor").with(eq(url)).will(returnValue(req));
        mockHttpRetrievalService.expects(once()).method("get").with(eq(req)).will(returnValue(new FakeHttpResponse()));
    }

    private final class FakeHttpResponse implements HttpResponse
    {

        public boolean isCached()
        {
            return false;
        }

        public boolean isFailed()
        {
            return false;
        }

        public boolean isNotFound()
        {
            return false;
        }

        public boolean isNotPermitted()
        {
            return false;
        }

        public InputStream getResponse() throws IOException
        {
            return getResourceAsStream("jiraResponse.xml");
        }

        public String getContentType()
        {
            return null;
        }

        public String[] getHeaders(String s)
        {
            return new String[0];
        }

        public String getStatusMessage()
        {
            return null;
        }

        public int getStatusCode()
        {
            return 200;
        }

        public void finish()
        {
            // do nothing
        }
    }

    String expectedJson = "{\n"+
        "page: 1,\n"+
        "total: 1,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','<img src=\"http://localhost:8080/images/icons/status_closed.gif\" alt=\"Closed\"/> Closed']}\n"+
        "\n"+
        "]}";

    String expectedJsonWithTotal = "{\n"+
        "page: 1,\n"+
        "total: 3,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','<img src=\"http://localhost:8080/images/icons/status_closed.gif\" alt=\"Closed\"/> Closed']}\n"+
        "\n"+
        "]}";

    String expectedJsonWithApostrophe = "{\n"+
        "page: 1,\n"+
        "total: 1,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'CONF-12242',cell:['<a href=\"http://jira.atlassian.com/browse/CONF-12242\" ><img src=\"http://jira.atlassian.com/images/icons/bug.gif\" alt=\"Bug\"/></a>','<a href=\"http://jira.atlassian.com/browse/CONF-12242\" >CONF-12242</a>','<a href=" +
        "\"http://jira.atlassian.com/browse/CONF-12242\" >Numbered List sub-items render differently in RSS versus browser</a>','David O\\'Flynn [Atlassian]','<img src=\"http://jira.atlassian.com/images/icons/status_open.gif\" alt=\"Open\"/> Open']}\n"+
        "\n"+
        "]}";

    String expectedJsonWithOddCharsAndNoMap = "{\n" + 
        "page: 1,\n" + 
        "total: 1,\n" + 
        "trustedMessage: null,\n" + 
        "rows: [\n" +
        "{id:'TST-7',cell:['<a href=\"http://localhost:8080/browse/TST-7\" ><img src=\"http://localhost:8080/images/icons/bug.gif\" alt=\"B&uuml;g\"/></a>','<a href=\"http://localhost:8080/browse/TST-7\" >TST-7</a>','<a href=" +
        "\"http://localhost:8080/browse/TST-7\" >test thing with lots of wierdness</a>','administrator','<img src=\"http://localhost:8080/images/icons/status_open.gif\" alt=\"New &amp; Improved\"/> New &amp; Improved']}\n" + 
        "\n" +
        "]}";
    
    String expectedJsonWithOddCharsAndIconMap = "{\n" + 
        "page: 1,\n" + 
        "total: 1,\n" + 
        "trustedMessage: null,\n" + 
        "rows: [\n" +
        "{id:'TST-7',cell:['<a href=\"http://localhost:8080/browse/TST-7\" ><img src=\"http://localhost:8080/images/icons/improvement.gif\" alt=\"B&uuml;g\"/></a>','<a href=\"http://localhost:8080/browse/TST-7\" >TST-7</a>','<a href=" +
        "\"http://localhost:8080/browse/TST-7\" >test thing with lots of wierdness</a>','administrator','<img src=\"http://localhost:8080/images/icons/improvement.gif\" alt=\"New &amp; Improved\"/> New &amp; Improved']}\n" + 
        "\n" +
        "]}";    
    
    String expectedJsonNoIconUrl = "{\n"+
        "page: 1,\n"+
        "total: 1,\n"+
        "trustedMessage: null,\n"+
        "rows: [\n"+
        "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','Closed']}\n"+
        "\n"+
        "]}";

    String expectedJsonJsReporter = "{\n" +
        "page: 1,\n" +
        "total: 1,\n" +
        "trustedMessage: null,\n" +
        "rows: [\n" +
        "{id:'TST-16327',cell:['Thomas&quot;&lt;script&gt;alert(1)&lt;\\/script&gt;']}\n" +
        "\n" +
        "]}";
}