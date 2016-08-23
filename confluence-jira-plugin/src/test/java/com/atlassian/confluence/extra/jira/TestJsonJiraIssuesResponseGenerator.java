package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.UserI18NBeanFactory;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.mail.internet.MailDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJsonJiraIssuesResponseGenerator extends TestCase
{

    @Mock
    private UserI18NBeanFactory i18NBeanFactory;

    @Mock
    private JiraIssuesManager jiraIssuesManager;

    @Mock
    private JiraIssuesColumnManager jiraIssuesColumnManager;

    @Mock
    private JiraIssuesManager.Channel channel;

    @Mock
    private I18NBean i18NBean;

    @Mock
    private TrustedConnectionStatus trustedConnectionStatus;

    @Mock
    private Element element;

    @Mock
    private Element linkElement;

    @Mock
    private Element customFieldsElement;

    @Mock
    private ReadOnlyApplicationLink applicationLink;

    private JiraIssuesDateFormatter jiraIssuesDateFormatter = new DefaultJiraIssuesDateFormatter();

    private List<String> columnNames;

    private JsonJiraIssuesResponseGenerator jsonJiraIssuesResponseGenerator;

    private String url;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);
        when(i18NBean.getText(anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return (String) invocationOnMock.getArguments()[0];
                    }
                }
        );

        when(element.getChild("link")).thenReturn(linkElement);
        when(element.getName()).thenReturn("item");
        when(element.getChild("customfields")).thenReturn(customFieldsElement);

        jsonJiraIssuesResponseGenerator = new JsonJiraIssuesResponseGenerator()
        {
        	@Override
        	public Locale getUserLocale()
        	{
        		return Locale.getDefault();
        	}
        };

        columnNames = Arrays.asList("type", "key", "summary", "reporter", "status");
        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000";

        when(applicationLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));
        when(applicationLink.getRpcUrl()).thenReturn(URI.create("http://rpcurl.com"));

    }

    public void testHandlesAnyChannel()
    {
        assertTrue(jsonJiraIssuesResponseGenerator.handles(null));
        assertTrue(jsonJiraIssuesResponseGenerator.handles(channel));
    }

    private Element getJiraIssuesXmlResponseChannelElement(String classpathResource) throws IOException, JDOMException
    {
        InputStream in = null;

        try
        {
            in = getClass().getClassLoader().getResourceAsStream(classpathResource);

            SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Document document = saxBuilder.build(in);

            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public void testConvertJiraResponseToJson() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("jiraResponse.xml"), null);
        String expectedJson = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','<img src=\"http://localhost:8080/images/icons/status_closed.gif\" alt=\"Closed\"/> Closed']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJson, json);

        // test with showCount=true
        String jsonCount = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, true, true);
        assertEquals("1", jsonCount);
    }


    // load other (newer) version of issues xml view
    public void testConvertJiraResponseToJsonWithTotal() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("jiraResponseWithTotal.xml"), null);
        String expectedJsonWithTotal = "{\n" +
                "page: 1,\n" +
                "total: 3,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'SOM-3',cell:['<a href=\"http://localhost:8080/browse/SOM-3\" ><img src=\"http://localhost:8080/images/icons/task.gif\" alt=\"Task\"/></a>','<a href=\"http://localhost:8080/browse/SOM-3\" >SOM-3</a>','<a href=\"http://localhost:8080/browse/SOM-3\" >do it</a>','A. D. Ministrator','<img src=\"http://localhost:8080/images/icons/status_closed.gif\" alt=\"Closed\"/> Closed']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJsonWithTotal, json);

        // test with showCount=true
        json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, true, true);
        assertEquals("3", json);
    }

    // load other (newer) version of issues xml view, with an apostrophe
    public void testConvertJiraResponseToJsonWithApostrophe() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("jiraResponseWithApostrophe.xml"), null);

        String expectedJsonWithApostrophe = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'CONF-12242',cell:['<a href=\"http://jira.atlassian.com/browse/CONF-12242\" ><img src=\"http://jira.atlassian.com/images/icons/bug.gif\" alt=\"Bug\"/></a>','<a href=\"http://jira.atlassian.com/browse/CONF-12242\" >CONF-12242</a>','<a href=" +
                "\"http://jira.atlassian.com/browse/CONF-12242\" >Numbered List sub-items render differently in RSS versus browser</a>','David O\\'Flynn [Atlassian]','<img src=\"http://jira.atlassian.com/images/icons/status_open.gif\" alt=\"Open\"/> Open']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJsonWithApostrophe, json);
    }


    // load other (newer) version of issues xml view, with an ampersand and an oomlaut
    public void testConvertJiraResponseToJsonWithOddCharacters() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("jiraResponseWithOddCharacters.xml"), null);

        String expectedJsonWithOddCharsAndNoMap = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TST-7',cell:['<a href=\"http://localhost:8080/browse/TST-7\" ><img src=\"http://localhost:8080/images/icons/bug.gif\" alt=\"B&uuml;g\"/></a>','<a href=\"http://localhost:8080/browse/TST-7\" >TST-7</a>','<a href=" +
                "\"http://localhost:8080/browse/TST-7\" >test thing with lots of wierdness</a>','administrator','<img src=\"http://localhost:8080/images/icons/status_open.gif\" alt=\"New &amp; Improved\"/> New &amp; Improved']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJsonWithOddCharsAndNoMap, json);

    }

    // load issues xml view that contains a javascript alert
    public void testConvertJiraResponseToJsonJsReporter() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("jiraResponseJsReporter.xml"), null);
        String expectedJsonJsReporter = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TST-16327',cell:['Thomas&quot;&lt;script&gt;alert(1)&lt;\\/script&gt;']}\n" +
                "\n" +
                "]}";

        columnNames = new ArrayList<String>();
        columnNames.add("reporter");
        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, false);
        assertEquals(expectedJsonJsReporter, json);
    }


    public void testDescriptionNotHtmlEncodedForApplinks() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("CONFJIRA-128.xml"), null);


        columnNames = new ArrayList<String>();
        columnNames.add("description");

        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals("{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TP-1',cell:['<b>This is bold text<\\/b>']}\n" + /* HTML not encoded */
                "\n" +
                "]}", json);
    }


    public void testDescriptionHtmlEncodedForNonApplinks() throws Exception
    {
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("CONFJIRA-128.xml"), null);


        columnNames = new ArrayList<String>();
        columnNames.add("description");

        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, false);
        assertEquals("{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TP-1',cell:['&lt;b&gt;This is bold text&lt;\\/b&gt;']}\n" + /* HTML is encoded for non-applinks*/
                "\n" +
                "]}", json);
    }

    /**
     * @see javax.mail.internet.MailDateFormat
     */
    public void testCustomFieldValueNotInterpretedAsStringIfItIsNotInMailDateFormat() throws Exception
    {
        String customFieldName = "Imaginary Non Date Field";
        Element customFieldElement = mock(Element.class);
        Element customFieldNameElement = mock(Element.class);
        Element customFieldValuesElement = mock(Element.class);
        Element customFieldValueElement = mock(Element.class);
        String customFieldValue = "Foobarbaz";

        when(linkElement.getValue()).thenReturn("http://localhost:1992/jira/browse/TST-1");
        when(customFieldsElement.getChildren()).thenReturn(
                Arrays.asList(
                        customFieldElement
                )
        );

        when(customFieldElement.getChild("customfieldname")).thenReturn(customFieldNameElement);
        when(customFieldNameElement.getValue()).thenReturn(customFieldName);
        when(customFieldElement.getAttributeValue("id")).thenReturn("customfield_10000");

        when(customFieldElement.getChild("customfieldvalues")).thenReturn(customFieldValuesElement);

        when(customFieldValuesElement.getChildren()).thenReturn(Arrays.asList(customFieldValueElement));
        when(customFieldValueElement.getValue()).thenReturn(customFieldValue);


        String jsonElement = jsonJiraIssuesResponseGenerator.getElementJson(
                element,
                Arrays.asList(customFieldName),
                new HashMap<String, String>(),
                true);


        assertEquals("{id:'',cell:['" + customFieldValue + " ']}", StringUtils.trim(jsonElement));
    }

    /**
     * @see javax.mail.internet.MailDateFormat
     */
    public void testCustomFieldInterpretedAsDateIfInMailDateFormat() throws Exception
    {
        String customFieldName = "Resolution Date";
        String customFieldValue = "Tue, 31 Mar 2009 11:44:42 +0800 (MYT)";
        Element customFieldElement = mock(Element.class);
        Element customFieldNameElement = mock(Element.class);
        Element customFieldValuesElement = mock(Element.class);
        Element customFieldValueElement = mock(Element.class);

        when(linkElement.getValue()).thenReturn("http://localhost:1992/jira/browse/TST-1");
        when(customFieldsElement.getChildren()).thenReturn(
                Arrays.asList(
                        customFieldElement
                )
        );

        when(customFieldElement.getChild("customfieldname")).thenReturn(customFieldNameElement);
        when(customFieldNameElement.getValue()).thenReturn(customFieldName);
        when(customFieldElement.getAttributeValue("id")).thenReturn("customfield_10000");

        when(customFieldElement.getChild("customfieldvalues")).thenReturn(customFieldValuesElement);

        when(customFieldValuesElement.getChildren()).thenReturn(Arrays.asList(customFieldValueElement));
        when(customFieldValueElement.getValue()).thenReturn(customFieldValue);

        jsonJiraIssuesResponseGenerator = new JsonJiraIssuesResponseGenerator()
        {
            @Override
            public Locale getUserLocale()
            {
                return Locale.getDefault();
            }
        };

        String jsonElement = jsonJiraIssuesResponseGenerator.getElementJson(
                element,
                Arrays.asList(customFieldName),
                new HashMap<String, String>(),
                true);

        assertEquals("{id:'',cell:['" + new SimpleDateFormat("dd/MMM/yy").format(new MailDateFormat().parse(customFieldValue)) + "']}", StringUtils.trim(jsonElement));
    }


    public void testCustomFieldHtmlEncodedForNonApplinks() throws Exception
    {
        String customFieldName = "Wikimarkup Custom Field";
        String customFieldValue = "<p>text with <b>bold</b> words.</p>";
        Element customFieldElement = mock(Element.class);
        Element customFieldNameElement = mock(Element.class);
        Element customFieldValuesElement = mock(Element.class);
        Element customFieldValueElement = mock(Element.class);

        when(linkElement.getValue()).thenReturn("http://localhost:1992/jira/browse/TST-1");
        when(customFieldsElement.getChildren()).thenReturn(
                Arrays.asList(
                        customFieldElement
                )
        );

        when(customFieldElement.getChild("customfieldname")).thenReturn(customFieldNameElement);
        when(customFieldNameElement.getValue()).thenReturn(customFieldName);
        when(customFieldElement.getAttributeValue("id")).thenReturn("customfield_10000");

        when(customFieldElement.getChild("customfieldvalues")).thenReturn(customFieldValuesElement);

        when(customFieldValuesElement.getChildren()).thenReturn(Arrays.asList(customFieldValueElement));
        when(customFieldValueElement.getValue()).thenReturn(customFieldValue);

        String jsonElement = jsonJiraIssuesResponseGenerator.getElementJson(
                element,
                Arrays.asList(customFieldName),
                new HashMap<String, String>(),
                false);

        assertEquals("{id:'',cell:['&lt;p&gt;text with &lt;b&gt;bold&lt;\\/b&gt; words.&lt;\\/p&gt; ']}", StringUtils.trim(jsonElement));
    }

    public void testCustomFieldNonHtmlEncodedForApplinks() throws Exception
    {
        String customFieldName = "Wikimarkup Custom Field";
        String customFieldValue = "<p>text with <b>bold</b> words.</p>";
        Element customFieldElement = mock(Element.class);
        Element customFieldNameElement = mock(Element.class);
        Element customFieldValuesElement = mock(Element.class);
        Element customFieldValueElement = mock(Element.class);

        when(linkElement.getValue()).thenReturn("http://localhost:1992/jira/browse/TST-1");
        when(customFieldsElement.getChildren()).thenReturn(
                Arrays.asList(
                        customFieldElement
                )
        );

        when(customFieldElement.getChild("customfieldname")).thenReturn(customFieldNameElement);
        when(customFieldNameElement.getValue()).thenReturn(customFieldName);
        when(customFieldElement.getAttributeValue("id")).thenReturn("customfield_10000");

        when(customFieldElement.getChild("customfieldvalues")).thenReturn(customFieldValuesElement);

        when(customFieldValuesElement.getChildren()).thenReturn(Arrays.asList(customFieldValueElement));
        when(customFieldValueElement.getValue()).thenReturn(customFieldValue);

        String jsonElement = jsonJiraIssuesResponseGenerator.getElementJson(
                element,
                Arrays.asList(customFieldName),
                new HashMap<String, String>(),
                true);

        assertEquals("{id:'',cell:['<p>text with <b>bold<\\/b> words.<\\/p> ']}", StringUtils.trim(jsonElement));
    }

    private class JsonJiraIssuesResponseGenerator extends com.atlassian.confluence.extra.jira.JsonFlexigridResponseGenerator
    {
        private JsonJiraIssuesResponseGenerator()
        {
            super(i18NBeanFactory, jiraIssuesManager, jiraIssuesColumnManager, jiraIssuesDateFormatter);
        }
    }

    public void testConvertJiraResponseToJsonWithDateInSameLocale() throws Exception
    {
    	 jsonJiraIssuesResponseGenerator = new JsonJiraIssuesResponseGenerator()
         {
         	@Override
         	public Locale getUserLocale()
         	{
         		return Locale.FRANCE;
         	}
         };
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("CONFJIRA-214.xml"), null);

        columnNames = Arrays.asList("type", "key", "summary", "reporter", "status", "created", "updated", "due", "submit date");

        String expectedJsonWithDateInDifferentLocale = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TST-7',cell:['<a href=\"http://localhost:8080/browse/TST-7\" ><img src=\"http://localhost:8080/images/icons/bug.gif\" alt=\"B&uuml;g\"/></a>','<a href=\"http://localhost:8080/browse/TST-7\" >TST-7</a>','<a href=" +
                "\"http://localhost:8080/browse/TST-7\" >A test issue with date in different Locale</a>','administrator','<img src=\"http://localhost:8080/images/icons/status_open.gif\" alt=\"New &amp; Improved\"/> New &amp; Improved'," +
                "'31/oct./11','04/nov./11','31/mai/12','27/juin/12']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJsonWithDateInDifferentLocale, json);

    }

    public void testConvertJiraResponseToJsonWithDueDateInDifferentLocale() throws Exception
    {
        jsonJiraIssuesResponseGenerator = new JsonJiraIssuesResponseGenerator()
        {
            @Override
            public Locale getUserLocale()
            {
                return Locale.FRANCE;
            }
        };
        JiraIssuesManager.Channel channel = new JiraIssuesManager.Channel(url, getJiraIssuesXmlResponseChannelElement("CONFJIRA-214-2.xml"), null);

        columnNames = Arrays.asList("type", "key", "summary", "reporter", "status", "created", "updated", "due", "submit date","closed date","test text field");

        String expectedJsonWithDateInDifferentLocale = "{\n" +
                "page: 1,\n" +
                "total: 1,\n" +
                "trustedMessage: null,\n" +
                "rows: [\n" +
                "{id:'TST-8',cell:['<a href=\"http://localhost:8080/browse/TST-8\" ><img src=\"http://localhost:8080/images/icons/bug.gif\" alt=\"B&uuml;g\"/></a>','<a href=\"http://localhost:8080/browse/TST-8\" >TST-8</a>','<a href=" +
                "\"http://localhost:8080/browse/TST-8\" >A test issue with dates in different Locale</a>','administrator','<img src=\"http://localhost:8080/images/icons/status_open.gif\" alt=\"New &amp; Improved\"/> New &amp; Improved'," +
                "'31/oct./11','04/nov./11','31/mai/12','27/juin/12','27/juin/12','text text ']}\n" +
                "\n" +
                "]}";

        // test with showCount=false
        String json = jsonJiraIssuesResponseGenerator.generate(channel, columnNames, 1, false, true);
        assertEquals(expectedJsonWithDateInDifferentLocale, json);

    }
}
