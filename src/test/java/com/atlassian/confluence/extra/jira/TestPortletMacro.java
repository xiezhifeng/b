package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

public class TestPortletMacro extends TestCase
{
    public void testUseLinkTagForStylesheetImports()
    {
        PortletMacro portletMacro = new PortletMacro();

        String initialPortletHtml = "<style type=\"text/css\" media=\"screen\">@import \"styles/combined.css\";</style>\n" +
            "<style type=\"text/css\" media=\"screen\">@import \"global.css\";</style>\n" +
            "<p>i'm a portlet</p>";
        String expectedTransformedPortletHtml = "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />\n" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"global.css\" />\n" +
            "<p>i'm a portlet</p>";
        assertEquals(expectedTransformedPortletHtml,portletMacro.useLinkTagForStylesheetImports(initialPortletHtml));

        String initialPortletHtmlWithSingleQuotes = "<p>i'm a single quote portlet</p>\n" +
            "<style type=\"text/css\" media=\"screen\">@import 'styles/combined.css';</style>";
        String expectedTransformedPortletHtmlWithSingleQuotes = "<p>i'm a single quote portlet</p>\n" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/combined.css\" />";
        assertEquals(expectedTransformedPortletHtmlWithSingleQuotes,portletMacro.useLinkTagForStylesheetImports(initialPortletHtmlWithSingleQuotes));

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

}
