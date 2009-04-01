package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.importexport.resource.DownloadResourceWriter;
import com.atlassian.confluence.importexport.resource.ExportDownloadResourceManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.renderer.v2.macro.MacroException;
import junit.framework.TestCase;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestPortletMacro extends TestCase
{
    private PortletMacro portletMacro;

    private ExportDownloadResourceManager exportDownloadResourceManager;

    private HttpRetrievalService httpRetrievalService;

    private SettingsManager settingsManager;

    private Map<String, String> macroParams;

    private HttpRequest httpRequest;

    private HttpResponse httpResponse;

    private Page pageToBeRenderedOn;

    private Map<String, Object> macroVelocityContext;

    private Settings globalSettings;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        exportDownloadResourceManager = mock(ExportDownloadResourceManager.class);
        httpRetrievalService = mock(HttpRetrievalService.class);

        globalSettings = new Settings();
        globalSettings.setDefaultEncoding("UTF-8");

        settingsManager = mock(SettingsManager.class);
        when(settingsManager.getGlobalSettings()).thenReturn(globalSettings);
        

        portletMacro = new PortletMacro();
        wireMacroDependencies();

        macroParams = new HashMap<String, String>();

        httpRequest = mock(HttpRequest.class);
        httpResponse = mock(HttpResponse.class);

        pageToBeRenderedOn = new Page();
        pageToBeRenderedOn.setSpace(new Space("tst"));

        macroVelocityContext = new HashMap<String, Object>();
    }

    private void wireMacroDependencies()
    {
        portletMacro.setExportDownloadResourceManager(exportDownloadResourceManager);
        portletMacro.setHttpRetrievalService(httpRetrievalService);
        portletMacro.setSettingsManager(settingsManager);
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

    public void testPortletHtmlExportedAsDownloadResourceAndMacroKnowsWhereToReferenceItInVelocity() throws MacroException, IOException
    {
        final String macroOutput = "foo";
        final String url = "http://jira.atlassian.com/secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:projectstats&projectid=10000&showclosed=false&sortDirection=asc&sortOrder=natural&statistictype=allFixfor";
        String html = "<html><head><title>foo</title></head><body>bar</body></html>";
        byte[] htmlBytes = html.getBytes("UTF-8");
        DownloadResourceWriter downloadResourceWriter;
        final String resourcePath = "/foo/bar.html";
        ByteArrayOutputStream downloadResourceWriterOutputStream = new ByteArrayOutputStream();

        portletMacro = new PortletMacro()
        {
            @Override
            protected String renderMacro(Map<String, Object> contextMap)
            {
                assertEquals(resourcePath, contextMap.get("iframeSourcePath"));

                return macroOutput;
            }

            @Override
            protected Map<String, Object> getMacroVelocityContext()
            {
                return macroVelocityContext;
            }
        };

        wireMacroDependencies();

        downloadResourceWriter = mock(DownloadResourceWriter.class);

        when(httpRetrievalService.getDefaultRequestFor(url)).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(new ByteArrayInputStream(htmlBytes));
        when(exportDownloadResourceManager.getResourceWriter(anyString(), anyString(), anyString())).thenReturn(downloadResourceWriter);
        when(downloadResourceWriter.getResourcePath()).thenReturn(resourcePath);
        when(downloadResourceWriter.getStreamForWriting()).thenReturn(downloadResourceWriterOutputStream);

        macroParams.put("url", url);

        assertEquals(macroOutput, portletMacro.execute(macroParams, null, pageToBeRenderedOn.toPageContext()));
        assertEquals(html, new String(downloadResourceWriterOutputStream.toByteArray(), "UTF-8"));
    }

    public void testHtmlProperlyDecodedUsingCharsetFromContentTypeHeader() throws MacroException, IOException
    {
        final String macroOutput = "foo";
        final String url = "http://jira.atlassian.com/secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:projectstats&projectid=10000&showclosed=false&sortDirection=asc&sortOrder=natural&statistictype=allFixfor";
        String html = "<html><head><title>foo</title></head><body>" + "\u201e\u00c9\u00ea\u201e\u00c7\u221e01" +  "<br />" + "\u00cf\u00c9\u00e0\u00ce\u00b0\u00fa\u00cf\u00f6\u00a5 \u00cd\u220f\u221e\u00ce\u00e4\u2022 01" +  "</body></html>";
        byte[] htmlBytes = html.getBytes("UTF-8");
        DownloadResourceWriter downloadResourceWriter;
        final String resourcePath = "/foo/bar.html";
        ByteArrayOutputStream downloadResourceWriterOutputStream = new ByteArrayOutputStream();

        portletMacro = new PortletMacro()
        {
            @Override
            protected String renderMacro(Map<String, Object> contextMap)
            {
                assertEquals(resourcePath, contextMap.get("iframeSourcePath"));

                return macroOutput;
            }

            @Override
            protected Map<String, Object> getMacroVelocityContext()
            {
                return macroVelocityContext;
            }
        };

        wireMacroDependencies();

        downloadResourceWriter = mock(DownloadResourceWriter.class);

        when(httpRetrievalService.getDefaultRequestFor(url)).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(new ByteArrayInputStream(htmlBytes));
        when(httpResponse.getHeaders("Content-Type")).thenReturn(new String[] { "text/html;charset=UTF-8" });
        when(exportDownloadResourceManager.getResourceWriter(anyString(), anyString(), anyString())).thenReturn(downloadResourceWriter);
        when(downloadResourceWriter.getResourcePath()).thenReturn(resourcePath);
        when(downloadResourceWriter.getStreamForWriting()).thenReturn(downloadResourceWriterOutputStream);

        macroParams.put("url", url);

        assertEquals(macroOutput, portletMacro.execute(macroParams, null, pageToBeRenderedOn.toPageContext()));
        assertEquals(html, new String(downloadResourceWriterOutputStream.toByteArray(), "UTF-8"));
    }

    public void testHtmlProperlyDecodedUsingCharsetInDocument() throws MacroException, IOException
    {
        final String macroOutput = "foo";
        final String url = "http://jira.atlassian.com/secure/RunPortlet.jspa?portletKey=com.atlassian.jira.plugin.system.portlets:projectstats&projectid=10000&showclosed=false&sortDirection=asc&sortOrder=natural&statistictype=allFixfor";
        String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>foo</title></head><body>" + "\u201e\u00c9\u00ea\u201e\u00c7\u221e01" +  "<br />" + "\u00cf\u00c9\u00e0\u00ce\u00b0\u00fa\u00cf\u00f6\u00a5 \u00cd\u220f\u221e\u00ce\u00e4\u2022 01" +  "</body></html>";
        byte[] htmlBytes = html.getBytes("UTF-8");
        DownloadResourceWriter downloadResourceWriter;
        final String resourcePath = "/foo/bar.html";
        ByteArrayOutputStream downloadResourceWriterOutputStream = new ByteArrayOutputStream();

        portletMacro = new PortletMacro()
        {
            @Override
            protected String renderMacro(Map<String, Object> contextMap)
            {
                assertEquals(resourcePath, contextMap.get("iframeSourcePath"));

                return macroOutput;
            }

            @Override
            protected Map<String, Object> getMacroVelocityContext()
            {
                return macroVelocityContext;
            }
        };

        wireMacroDependencies();

        downloadResourceWriter = mock(DownloadResourceWriter.class);

        when(httpRetrievalService.getDefaultRequestFor(url)).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(new ByteArrayInputStream(htmlBytes));
        when(exportDownloadResourceManager.getResourceWriter(anyString(), anyString(), anyString())).thenReturn(downloadResourceWriter);
        when(downloadResourceWriter.getResourcePath()).thenReturn(resourcePath);
        when(downloadResourceWriter.getStreamForWriting()).thenReturn(downloadResourceWriterOutputStream);

        macroParams.put("url", url);

        assertEquals(macroOutput, portletMacro.execute(macroParams, null, pageToBeRenderedOn.toPageContext()));
        assertEquals(html, new String(downloadResourceWriterOutputStream.toByteArray(), "UTF-8"));
    }
}
