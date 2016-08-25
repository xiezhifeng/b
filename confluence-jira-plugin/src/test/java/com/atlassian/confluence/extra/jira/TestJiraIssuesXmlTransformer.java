package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import javax.mail.internet.MailDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TestJiraIssuesXmlTransformer extends TestCase
{
    private SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
    private JiraIssuesXmlTransformer transformer = new JiraIssuesXmlTransformer();

    private Element itemElement;

    public TestJiraIssuesXmlTransformer(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        InputStream stream = getResourceAsStream("jiraResponseWithComplexFields.xml");
        Document document = saxBuilder.build(stream);
        itemElement = (Element) XPath.selectSingleNode(document, "/rss//channel//item");
    }

    private InputStream getResourceAsStream(String name) throws IOException
    {
        URL url = getClass().getClassLoader().getResource(name);
        return url.openStream();
    }

    public void testIdentityXForm()
    {
        Element titleElement = itemElement.getChild("title");
        Element xFormedElement = transformer.collapseMultiple(itemElement, "title" );
        assertEquals( "Identity tranformation on single-valued attributes", titleElement.getValue(), xFormedElement.getValue());
    }

    public void testBuiltins()
    {
        Element titleElement = itemElement.getChild("title");
        Element xFormedElement = transformer.findSimpleBuiltinField(itemElement, "title");
        assertEquals( "Correctly found builtin", titleElement.getValue(), xFormedElement.getValue());

        xFormedElement = transformer.findSimpleBuiltinField(itemElement, "Labels");
        assertNull("Shouldn't find custom fields", xFormedElement);
    }

    // test multiple top-level items, e.g., versions and components
    public void testMultiples()
    {
        Element xFormedElement = transformer.collapseMultiple(itemElement, "version" );
        assertEquals( "Collapsing multiple version tags", expectedVersions, xFormedElement.getValue());

        xFormedElement = transformer.collapseMultiple(itemElement, "component" );
        assertEquals( "Collapsing multiple component tags", expectedComponents, xFormedElement.getValue());

        xFormedElement = transformer.collapseMultiple(itemElement, "" );
        assertTrue( "Collapsing empty tag", StringUtils.isBlank(xFormedElement.getValue()));
    }

    public void testCustomFields()
    {
        Element titleElement = itemElement.getChild("title");
        Element xFormedElement = transformer.valueForField(itemElement, "title" );
        assertEquals( "valueForField should defer to builtins", titleElement.getValue(), xFormedElement.getValue());

        xFormedElement = transformer.valueForField(itemElement, "Labels");
        assertEquals( "Should find custom field", expectedLabels, xFormedElement.getValue().trim());

        xFormedElement = transformer.valueForField(itemElement, "Nonexistant Field");
        assertTrue( "Shouldn't find invalid field", StringUtils.isBlank(xFormedElement.getValue()));
    }

    public void testMissingCustomFields() throws Exception
    {
        InputStream stream = getResourceAsStream("jiraResponse.xml");
        Document document = saxBuilder.build(stream);
        Element item2 = (Element) XPath.selectSingleNode(document, "/rss//channel//item");

        Element xFormedElement = transformer.valueForField(item2, "Labels");
        assertTrue( "Should handle missing customsfields tag", StringUtils.isBlank(xFormedElement.getValue()));

    }

    public void testAccumulatingColumnMap()
    {
        Map fieldMap = new HashMap();
        Element xFormedElement = transformer.valueForField(itemElement, "Labels", fieldMap);
        assertTrue( "Should accumulate labels", fieldMap.containsKey("Labels"));
        assertTrue( "Should create correct mapping", fieldMap.get("Labels").equals("customfield_10030"));
    }

    public void testAbsentAttributes() throws Exception
    {
        InputStream stream = getResourceAsStream("jiraResponse.xml");
        Document document = saxBuilder.build(stream);
        Element item2 = (Element) XPath.selectSingleNode(document, "/rss//channel//item");

        Element xFormedElement = transformer.collapseMultiple(item2, "version" );
        assertEquals( "Collapsing multiple version tags", "", xFormedElement.getValue());

        // Comments tag is missing
        xFormedElement = transformer.collapseMultiple(item2, "comments");
        assertEquals( "Converted multiple comments", "", xFormedElement.getValue());

        // Attachments tag is present but has no children
        xFormedElement = transformer.collapseMultiple(item2, "attachments");
        assertEquals( "Converted multiple attachments", "", xFormedElement.getValue());
    }

    // test nested attributes e.g., comments and attachments
    public void testNested()
    {
        Element xFormedElement = transformer.collapseMultiple(itemElement, "comments");
        assertEquals( "Converted multiple comments", "3", xFormedElement.getValue());

        xFormedElement = transformer.collapseMultiple(itemElement, "attachments");
        assertEquals( "Converted multiple attachments", "2", xFormedElement.getValue());
    }

    public void testCustomFieldDateValueFormattedNicely() throws IOException, JDOMException, ParseException
    {
        InputStream stream = null;

        try
        {
            stream = getResourceAsStream("jiraResponseWithDateCustomField.xml");
            Document document = saxBuilder.build(stream);
            itemElement = (Element) XPath.selectSingleNode(document, "/rss//channel//item");
            DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yy");
            assertEquals(
                    dateFormat.format(new MailDateFormat().parse("Wed, 16 Sep 2009 21:34:45 -0500 (CDT)")),
                    transformer.valueForFieldDateFormatted(itemElement, "Date of First Response", dateFormat, null));
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
    }

    public void testCustomFieldDateJPValueFormattedNicely() throws IOException, JDOMException, ParseException
    {
        InputStream stream = null;

        try
        {
            stream = getResourceAsStream("jiraResponseWithJPDateCustomField.xml");
            Document document = saxBuilder.build(stream);
            itemElement = (Element) XPath.selectSingleNode(document, "/rss//channel//item");
            DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yy");
            assertEquals(
                    dateFormat.format(new MailDateFormat().parse("Sun, 6 Nov 2014 14:11:07 +0700")),
                    transformer.valueForFieldDateFormatted(itemElement, "Date of First Response", dateFormat, Locale.JAPAN));
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
    }

    public void testCustomFieldStringValueNotFormatted()
    {
        assertEquals(expectedLabels, transformer.valueForFieldDateFormatted(itemElement, "Labels", new SimpleDateFormat("dd/MMM/yy"), null).trim());
    }


    private static final String expectedVersions = "2.8.1, 2.8.2, 2.8.3, 2.8.4, 2.8.5";
    private static final String expectedComponents = "{jiraissues}";
    private static final String expectedLabels = "crash pdf export";

}
