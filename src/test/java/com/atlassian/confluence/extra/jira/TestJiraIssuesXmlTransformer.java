package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jmock.MockObjectTestCase;

public class TestJiraIssuesXmlTransformer extends MockObjectTestCase
{
    private SAXBuilder saxBuilder = new SAXBuilder(JiraIssuesUtils.SAX_PARSER_CLASS);
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

    public void testBuiltinRecognition()
    {
        assertTrue("Find builtin name", transformer.isColumnBuiltIn("description"));
        assertFalse("Should not find custom field", transformer.isColumnBuiltIn("Labels"));
    }
    
    public void testCanonicalization()
    {
        assertEquals( "Find builtin with correct name", "description", transformer.findBuiltinCanonicalForm("description"));
        assertEquals( "Find builtin with incorrect form", "description", transformer.findBuiltinCanonicalForm("Description"));
        assertEquals( "Find builtin with incorrect form", "fixVersion", transformer.findBuiltinCanonicalForm("fixversion"));
        assertEquals( "Leave non-builtins alone", "Labels", transformer.findBuiltinCanonicalForm("Labels"));
        assertFalse( "Leave non-builtins alone", "Labels".equals(transformer.findBuiltinCanonicalForm("labels")));
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
    
    
    private static final String expectedVersions = "2.8.1, 2.8.2, 2.8.3, 2.8.4, 2.8.5";
    private static final String expectedComponents = "{jiraportlet}, {jiraissues}";
    private static final String expectedLabels = "crash pdf export";

}
