/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 15, 2004
 * Time: 3:23:53 PM
 */
package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.importexport.resource.DownloadResourceWriter;
import com.atlassian.confluence.importexport.resource.WritableDownloadResourceManager;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.user.User;
import com.opensymphony.util.TextUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.w3c.tidy.Tidy;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PortletMacro extends BaseMacro implements Macro
{
    private static final Logger logger = Logger.getLogger(PortletMacro.class);

    /**
     * Maximum length of a URL schema, e.g. "https://"
     */
    protected static final int URL_SCHEMA_LENGTH = 8;

    private static final Pattern CHARSET_IN_CONTENT_TYPE_HEADER_PATTERN = Pattern.compile("charset\\s*=\\s*(.+)\\s*$");

    private HttpRetrievalService httpRetrievalService;

    private TrustedTokenAuthenticator trustedTokenAuthenticator;

    private SettingsManager settingsManager;

    private WritableDownloadResourceManager writableDownloadResourceManager;

    public void setExportDownloadResourceManager(WritableDownloadResourceManager writableDownloadResourceManager)
    {
        this.writableDownloadResourceManager = writableDownloadResourceManager;
    }

    public void setHttpRetrievalService(HttpRetrievalService httpRetrievalService)
    {
        this.httpRetrievalService = httpRetrievalService;
    }

    public void setTrustedTokenFactory(TrustedTokenFactory trustedTokenFactory)
    {
        this.trustedTokenAuthenticator = new TrustedTokenAuthenticator(trustedTokenFactory);
    }

    public void setSettingsManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }

   
    public boolean isInline()
    {
        return false;
    }

    public boolean hasBody()
    {
        return false;
    }

    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }

    private String getIframeSourcePath(String portletHtml) throws IOException
    {
        InputStream in = null;
        OutputStream out = null;

        try
        {
            User user = AuthenticatedUserThreadLocal.getUser();
            DownloadResourceWriter downloadResourceWriter;

            downloadResourceWriter = writableDownloadResourceManager.getResourceWriter(
                    null == user ? null : user.getName(),
                    getName(),
                    ".html");

            in = new ByteArrayInputStream(portletHtml.getBytes(settingsManager.getGlobalSettings().getDefaultEncoding()));
            out = downloadResourceWriter.getStreamForWriting();

            IOUtils.copy(in, out);


            return downloadResourceWriter.getResourcePath();
        }
        finally
        {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    public String execute(Map map, String s, RenderContext renderContext) throws MacroException
    {
        try 
        {
			return execute((Map<String, String>) map, s, new DefaultConversionContext(renderContext));
		} 
        catch (MacroExecutionException e) 
		{
			throw new MacroException(e.getMessage());
		}
    }

    ///CLOVER:OFF
    protected Map<String, Object> getMacroVelocityContext()
    {
        return MacroUtils.defaultVelocityContext();
    }

    protected String renderMacro(Map<String, Object> contextMap)
    {
        return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraportlet.vm", contextMap);
    }
    ///CLOVER:ON

    /*
    changes css imports in the form
    <style type="text/css" media="screen">@import "styles/combined.css";</style>
    to the form
    <link rel="stylesheet" type="text/css" href="styles/combined.css" />
    to prevent triggering an IE bug -- see CONFJIRA-103
     */
    protected String useLinkTagForStylesheetImports(String portletHtml) throws MalformedStyleException
    {
        int fromIndex = 0;
        int styleTagIndex;
        StringBuilder transformedHtml = new StringBuilder();
        
        while((styleTagIndex = portletHtml.indexOf("<style", fromIndex)) != -1)
        {
            // make sure there is a closing tag -- not just closing /> because we care about @imports inside the tag
            int styleClosingTagIndex = portletHtml.indexOf("</style>", styleTagIndex);
            if(styleClosingTagIndex==-1) // shouldn't really happen -- means open style tag that isn't closed
                throw new MalformedStyleException("Opening <style> tag has no closing </style> tag");
            
            transformedHtml.append(portletHtml.substring(fromIndex, styleTagIndex));

            int endOfOpeningStyleTag = portletHtml.indexOf(">", styleTagIndex);
            String styleTagAttributes = portletHtml.substring(styleTagIndex + "<style".length(), endOfOpeningStyleTag);
            String currentStyleTagBody = portletHtml.substring(endOfOpeningStyleTag + 1, styleClosingTagIndex);

            transformedHtml.append(transformStyleBody(styleTagAttributes, currentStyleTagBody));
            fromIndex = styleClosingTagIndex+"</style>".length(); // start searching again from closing style tag index + length of tag

        }
        transformedHtml.append(portletHtml.substring(fromIndex));
        return transformedHtml.toString();
    }

    private String transformStyleBody(String styleTagAttributes, String styleTagBody)
        throws MalformedStyleException
    {
        int importIndex = styleTagBody.indexOf("@import");
        // make sure found importIndex, otherwise this is just some irrelevant style tag
        if(importIndex == -1)
        {
            return buildStyleTag(styleTagAttributes, styleTagBody);
        }

        StringBuilder transformedStyles = new StringBuilder();
        String beforeImport = styleTagBody.substring(0, importIndex);
        if(StringUtils.isNotBlank(beforeImport))
        {
            transformedStyles.append(buildStyleTag(styleTagAttributes, beforeImport));
        }
        do
        {
            int endQuoteIndex = transformImportedStylesheetToLink(transformedStyles, styleTagBody, importIndex);

            importIndex = styleTagBody.indexOf("@import",  endQuoteIndex);

            String afterImport = styleTagBody.substring(endQuoteIndex+2, importIndex == -1 ? styleTagBody.length() : importIndex); // +2 because was using var as ending and now using as beginning AND semicolon
            if(StringUtils.isNotBlank(afterImport))
            {
                transformedStyles.append(buildStyleTag(styleTagAttributes, afterImport));
            }
        } while(importIndex != -1);
        return transformedStyles.toString();
    }

    private int transformImportedStylesheetToLink(StringBuilder transformedHtml, String currentStyleTag, int importIndex)
        throws MalformedStyleException
    {
        char quote = '\"';
        int quoteIndex = currentStyleTag.indexOf(quote,importIndex);
        if (quoteIndex==-1) // didn't find " so try single quotes instead
        {
            quote = '\'';
            quoteIndex = currentStyleTag.indexOf(quote,importIndex);

            // if still no quote, can't do this tag
            if(quoteIndex==-1)
                throw new MalformedStyleException("No opening quote import statement has no closing quote");
        }
        int endQuoteIndex = currentStyleTag.indexOf(quote,quoteIndex+1); // +1 because don't want to find same quote just got -- want to start after
        if (endQuoteIndex == -1)
            throw new MalformedStyleException("Opening quote in import statement has no closing quote");
        
        transformedHtml.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        transformedHtml.append(currentStyleTag.substring(quoteIndex+1,endQuoteIndex));
        transformedHtml.append("\" />");
        return endQuoteIndex;
    }

    private String buildStyleTag(String styleTagAttributes, String styles)
    {
        StringBuilder transformedHtml = new StringBuilder();
        transformedHtml.append("<style");
        transformedHtml.append(styleTagAttributes);
        transformedHtml.append(">");
        transformedHtml.append(styles);
        transformedHtml.append("</style>");
        return transformedHtml.toString();
    }

    final static class MalformedStyleException extends Exception
    {
        public MalformedStyleException(String message)
        {
            super(message);
        }
    }

    private String getPortletHtmlEncodingFromMetaTag(InputStream portletHtmlInputStream)
    {
        Writer errorWriter = null;

        try
        {
            Tidy tidy;
            Document document;
            Node contentTypeMetaNode;
            String contentCharset;

            tidy = new Tidy();
            tidy.setErrout(new PrintWriter(errorWriter = new StringWriter()));
            tidy.setQuiet(true);
            tidy.setForceOutput(true); /* We want output no matter what */
            tidy.setXHTML(true); /* We want XHTML output */

            document = new DOMReader().read(tidy.parseDOM(portletHtmlInputStream, null));
            contentTypeMetaNode = document.selectSingleNode("//head/meta[@http-equiv=\"Content-Type\"]/@content");
            contentCharset = null != contentTypeMetaNode ? contentTypeMetaNode.getText() : null;

            if (StringUtils.isNotBlank(contentCharset))
            {
                Matcher charsetMatcher = CHARSET_IN_CONTENT_TYPE_HEADER_PATTERN.matcher(contentCharset);
                if (charsetMatcher.find())
                    return charsetMatcher.group(1);
            }

            return null;
        }
        finally
        {
            if (null != errorWriter)
                logger.debug("Tidy errors: " + SystemUtils.LINE_SEPARATOR + errorWriter.toString());
            IOUtils.closeQuietly(errorWriter);
        }
    }

    private String getDecodedPortletHtml(HttpResponse httpResponse) throws IOException
    {
        InputStream in = null;

        try
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            in = httpResponse.getResponse();

            /* Read HTTP response into buffer so that it can be read multiple times */
            IOUtils.copy(in, byteArrayOutputStream);

            String[] headers = httpResponse.getHeaders("Content-Type");
            /* I don't expect to see more than one 'Content-Type' header in a single response, but if that does happen,
             * the first one will be chosen.
             */
            String contentTypeHeader  = headers != null && headers.length > 0 ? headers[0] : null;

            if (StringUtils.isNotBlank(contentTypeHeader))
            {
                Matcher charsetMatcher = CHARSET_IN_CONTENT_TYPE_HEADER_PATTERN.matcher(contentTypeHeader);
                if (charsetMatcher.find())
                    return IOUtils.toString(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), charsetMatcher.group(1));
            }

            /* Try to get the charset to decode the response from the metatag. If that does not exist, default to ISO-8859-1. */
            String charsetFromMetaTag = StringUtils.defaultString(getPortletHtmlEncodingFromMetaTag(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())), "ISO-8859-1");
            return IOUtils.toString(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), charsetFromMetaTag);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public String retrievePortletContent(String url, boolean useApplinks) throws IOException
    {
        HttpRequest req = httpRetrievalService.getDefaultRequestFor(url);
        if (useApplinks)
        {
            req.setAuthenticator(trustedTokenAuthenticator);
        }

        HttpResponse resp = httpRetrievalService.get(req);
        try
        {
            if (resp.isFailed())
                throw new RuntimeException(resp.getStatusMessage());

            return getDecodedPortletHtml(resp);
        }
        finally
        {
            resp.finish();
        }
    }

    protected String getParam(Map<String, Object> params, String paramName, int paramPosition)
    {
        String param = (String)params.get(paramName);
        if(param==null)
            param = StringUtils.defaultString((String)params.get(""+paramPosition));

        return param.trim();
    }

    // url needs its own method because in the v2 macros params with equals don't get saved into the map with numbered keys such as "0", unlike the old macros
    protected String getUrlParam(Map<String, Object> params)
    {
        String url = (String)params.get("url");
        if(url==null)
        {
            String allParams = (String)params.get(com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY);
            int barIndex = allParams.indexOf('|');
            if(barIndex!=-1)
                url = allParams.substring(0,barIndex);
            else
                url = allParams;
        }
        return cleanUrlParentheses(url.trim());
    }

    // for CONF-1672
    protected String cleanUrlParentheses(String url)
    {
        if (url.indexOf('(') > 0)
            url = url.replaceAll("\\(", "%28");

        if (url.indexOf(')') > 0)
            url = url.replaceAll("\\)", "%29");

        if (url.indexOf("&amp;") > 0)
            url = url.replaceAll("&amp;", "&");

        return url;
    }

    protected String fetchPageContent(Map macroParameterMap) throws IOException
    {
        String url = getUrlParam(macroParameterMap).trim();

        String baseUrl = (String)macroParameterMap.get("baseurl");
        if (!TextUtils.stringSet(baseUrl))
            baseUrl = url;

        String anonymousStr = TextUtils.noNull(getParam(macroParameterMap, "anonymous", 1)).trim();

        if ("".equals(anonymousStr))
            anonymousStr = "false";

        boolean useApplinks = !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);
        String result = retrievePortletContent(url, useApplinks);

        try
        {
            result = useLinkTagForStylesheetImports(result);
        } catch (MalformedStyleException e)
        {
            // do nothing -- use original result if can't transform it
        }
        return correctBaseUrls(result, baseUrl);
    }

    public String getName()
    {
        return "jiraportlet";
    }

    /**
     * Taken from UrlUtil, but we have to customise some parts. We have to also add the baseUrl to the
     * styles imports.
     */
    private String correctBaseUrls(String html, String baseUrl)
    {
        if (html.length() < 10)
        {
            return html;
        }

        StringBuffer result = new StringBuffer(html.length());

        int idx = 0;
        while (true)
        {
            String matchText = "";
            int matchIdx = html.length() + 1; // initialise beyond the end of the string

            String[] linkText = linksToFix();
            for (int i = 0; i < linkText.length; i++)
            {
                int testIdx = html.indexOf(linkText[i], idx);
                if (testIdx >= 0 && testIdx < matchIdx)
                {
                    matchText = linkText[i];
                    matchIdx = testIdx;
                }
                // don't exit early -- we need to find the match closest to the start of the string!
            }

            if (matchIdx > html.length()) // no match found
            {
                result.append(html.substring(idx));
                break;
            }

            matchIdx += matchText.length();
            result.append(html.substring(idx, matchIdx));

            String linkStart = html.substring(matchIdx, Math.min(matchIdx + URL_SCHEMA_LENGTH, html.length()));

            if (isLocalUrl(linkStart))
            {
                if (linkStart.startsWith("/"))
                    result.append(getServerUrl(baseUrl));
                else
                    result.append(getUrlPath(baseUrl)).append("/");
            }

            idx = matchIdx;
        }

        return result.toString();
    }

    private String[] linksToFix()
    {
        return new String[]{
            " href=\"",
            " href='",
            " src=\"",
            " src='",
            "@import \"",
            "@import '"
        };
    }

    /**
     * @param url should not be null
     * @return false if the link starts with a valid protocol (e.g. http://), otherwise true.
     */
    private boolean isLocalUrl(String url)
    {
        String[] validProtocols = new String[]{"http://", "https://", "mailto:", "ftp://", "javascr"};
        for (int i = 0; i < validProtocols.length; i++)
        {
            String validProtocol = validProtocols[i];
            if (url.startsWith(validProtocol)) return false;
        }
        return true;
    }

    /**
     * Returns the baseUrl, first removing the query parameters, then removing everything upto and including the last
     * slash.
     *
     * @param baseUrl should not be null, http://www.example.com/foo/bar?quux
     * @return the directory with a trailing slash if one was passed, e.g. http://www.example.com/foo
     */
    private String getUrlPath(String baseUrl)
    {
        String result = baseUrl;

        // strip query parameters
        if (result.indexOf('?') > 0)
            result = result.substring(0, result.indexOf('?'));

        // strip everything after last slash, excluding slashes in the URL schema
        int lastSlash = result.lastIndexOf('/');
        if (lastSlash >= URL_SCHEMA_LENGTH)
            result = result.substring(0, lastSlash);

        return result;
    }

    /**
     * Returns the baseUrl with everything after the first slash removed (excluding slashes in the URL schema, e.g.
     * "http://").
     *
     * @param baseUrl should not be null, e.g. http://www.example.com/foo/bar?quux
     * @return the absolute server URL with a trailing slash if one was passed, e.g. http://www.example.com/
     */
    private String getServerUrl(String baseUrl)
    {
        String result = baseUrl;

        // strip everything after first slash, excluding slashes in the URL schema
        int firstSlash = result.indexOf('/', URL_SCHEMA_LENGTH);
        if (firstSlash >= 0)
            result = result.substring(0, firstSlash);

        return result;
    }

	public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException 
	{
		String portletDataHtml;
        try
        {
            portletDataHtml = fetchPageContent(parameters);

            Map<String, Object> contextMap = getMacroVelocityContext();

            contextMap.put("iframeSourcePath", getIframeSourcePath(portletDataHtml));
            contextMap.put("portletDataHtml", portletDataHtml);
            contextMap.put("outputType", conversionContext.getEntity().toPageContext().getOutputType());

            return renderMacro(contextMap);
        }
        catch (IOException e) 
        {
			throw new MacroExecutionException(e);
		}
	}

	public BodyType getBodyType() 
	{
		return BodyType.NONE;
	}

	public OutputType getOutputType() 
	{
		return OutputType.INLINE;
	}
}