package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

public class TestPortletMacro extends TestCase
{
    private PortletMacro portletMacro;
    public void setUp()
    {
        portletMacro = new PortletMacro();
    }

    public void testUseLinkTagForStylesheetImports() throws PortletMacro.MalformedStyleException
    {
        String initialPortletHtml = "<style type=\"text/css\" media=\"screen\">@import \"styles/combined.css\";</style>\n" +
            "<style type=\"text/css\" media=\"screen\">@import \"global.css\";</style>\n" +
            "<p>i'm a portlet</p>";
        String expectedTransformedPortletHtml = "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />\n" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"global.css\" />\n" +
            "<p>i'm a portlet</p>";
        assertEquals(expectedTransformedPortletHtml,portletMacro.useLinkTagForStylesheetImports(initialPortletHtml));

        initialPortletHtml = "<style type=\"text/css\" media=\"screen\">  @import \"styles/combined.css\";</style><style type=\"text/css\" media=\"screen\">@import \"global.css\";</style>\n" +
            "<p>i'm a portlet</p>";
        expectedTransformedPortletHtml = "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" /><link rel=\"stylesheet\" type=\"text/css\" href=\"global.css\" />\n" +
            "<p>i'm a portlet</p>";
        assertEquals(expectedTransformedPortletHtml,portletMacro.useLinkTagForStylesheetImports(initialPortletHtml));

    }

    public void testUseLinkTagForStylesheetImportsWithSingleQuotes() throws PortletMacro.MalformedStyleException
    {
        String initialPortletHtmlWithSingleQuotes = "<p>i'm a single quote portlet</p>\n" +
            "<style type=\"text/css\" media=\"screen\">@import 'styles/combined.css';</style>";
        String expectedTransformedPortletHtmlWithSingleQuotes = "<p>i'm a single quote portlet</p>\n" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />";
        assertEquals(expectedTransformedPortletHtmlWithSingleQuotes,portletMacro.useLinkTagForStylesheetImports(initialPortletHtmlWithSingleQuotes));
    }

    public void testUseLinkTagForStylesheettImportsWithNonImport() throws PortletMacro.MalformedStyleException
    {
        String initialPortletHtmlWithNonimport = "<style type=\"text/css\">\n" +
            "h1 {color: red}\n" +
            "h3 {color: blue}\n" +
            "</style>\n" +
            "<style type=\"text/css\" media=\"screen\">@import \"styles/combined.css\";</style>\n" +
            "<p>i'm a portlet with one style tag with no import</p>";
        String expectedTransformedPortletHtmlWithNonimport = "<style type=\"text/css\">\n" +
            "h1 {color: red}\n" +
            "h3 {color: blue}\n" +
            "</style>\n" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />\n" +
            "<p>i'm a portlet with one style tag with no import</p>";
        assertEquals(expectedTransformedPortletHtmlWithNonimport,portletMacro.useLinkTagForStylesheetImports(initialPortletHtmlWithNonimport));
    }

    public void testUseLinkTagForStylesheettImportsWithCombinedImportAndStyles() throws PortletMacro.MalformedStyleException
    {
        String initialPortletHtmlWithCombinedImport = "<style type=\"text/css\"> @import \"styles/combined.css\"; h1 {color: red}; h3 {color: blue}; @import \"styles/combined.css\"; h1 {color: red}; h3 {color: blue}; </style>\n" +
            "<p>i'm a portlet with one style tag with import and styles</p>";
        String expectedTransformedPortletHtmlWithNonimport = "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" /><style type=\"text/css\"> h1 {color: red}; h3 {color: blue}; </style><link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" /><style type=\"text/css\"> h1 {color: red}; h3 {color: blue}; </style>\n" +
            "<p>i'm a portlet with one style tag with import and styles</p>";
        assertEquals(expectedTransformedPortletHtmlWithNonimport,portletMacro.useLinkTagForStylesheetImports(initialPortletHtmlWithCombinedImport));
        
//        String initialPortletHtml = "<style type=\"text/css\">\n" +
//            "@import \"styles/combined.css\";\n" +
//            "h1 {color: red}\n" +
//            "h3 {color: blue}\n" +
//            "</style>\n" +
//            "<p>i'm a portlet with one style tag with import and styles</p>";
//        String expectedTransformedPortletHtml = "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />\n" +
//            "<link rel=\"stylesheet\" type=\"text/css\" href=\"global.css\" />\n" +
//            "<p>i'm a portlet</p>";
//        assertEquals(expectedTransformedPortletHtml,portletMacro.useLinkTagForStylesheetImports(initialPortletHtml));
    }

}
