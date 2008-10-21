/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 15, 2004
 * Time: 3:23:53 PM
 */
package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.include.AbstractHttpRetrievalMacro;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.renderer.WikiRendererContextKeys;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.opensymphony.util.TextUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;
import java.util.Map;

public class PortletMacro extends AbstractHttpRetrievalMacro implements TrustedApplicationConfig
{
    /**
     * Maximum length of a URL schema, e.g. "https://"
     */
    protected static final int URL_SCHEMA_LENGTH = 8;

    private final TrustedApplicationConfig trustedApplicationConfig = new JiraIssuesTrustedApplicationConfig();

    protected String getHtml(MacroParameter macroParameter) throws IllegalArgumentException, IOException
    {
        String url = TextUtils.noNull(macroParameter.get("url", 0)).trim();
        url = cleanUrlParentheses(url);
        return fetchPageContent(url, macroParameter);
    }

    public void setTrustWarningsEnabled(boolean enabled)
    {
        trustedApplicationConfig.setTrustWarningsEnabled(enabled);
    }

    public void setUseTrustTokens(boolean enabled)
    {
        trustedApplicationConfig.setUseTrustTokens(enabled);
    }

    public boolean isTrustWarningsEnabled()
    {
        return trustedApplicationConfig.isTrustWarningsEnabled();
    }

    public boolean isUseTrustTokens()
    {
        return trustedApplicationConfig.isUseTrustTokens();
    }

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

            transformStyleBody(transformedHtml, styleTagAttributes, currentStyleTagBody);
            fromIndex = styleClosingTagIndex+"</style>".length(); // start searching again from closing style tag index + length of tag

        }
        transformedHtml.append(portletHtml.substring(fromIndex));
        return transformedHtml.toString();
    }

    private void transformStyleBody(StringBuilder transformedHtml, String styleTagAttributes, String styleTagBody)
        throws MalformedStyleException
    {
        int importIndex = styleTagBody.indexOf("@import");
        // make sure found importIndex, otherwise this is just some irrelevant style tag
        if(importIndex == -1)
        {
            addStyleTag(transformedHtml, styleTagAttributes, styleTagBody);
        }
        else
        {
            String beforeImport = styleTagBody.substring(0, importIndex);
            if(StringUtils.isNotBlank(beforeImport))
            {
                addStyleTag(transformedHtml, styleTagAttributes, beforeImport);
            }
            do
            {
                int endQuoteIndex = transformImportedStylesheetToLink(transformedHtml, styleTagBody, importIndex);

                importIndex = styleTagBody.indexOf("@import",  endQuoteIndex);

                String afterImport = styleTagBody.substring(endQuoteIndex+2, importIndex == -1 ? styleTagBody.length() : importIndex); // +2 because was using var as ending and now using as beginning AND semicolon
                if(StringUtils.isNotBlank(afterImport))
                {
                    addStyleTag(transformedHtml, styleTagAttributes, afterImport);
                }
            } while(importIndex != -1);
        }
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

    private void addStyleTag(StringBuilder transformedHtml, String styleTagAttributes, String styles)
    {
        transformedHtml.append("<style");
        transformedHtml.append(styleTagAttributes);
        transformedHtml.append(">");
        transformedHtml.append(styles);
        transformedHtml.append("</style>");
    }

    final static class MalformedStyleException extends Exception
    {
        public MalformedStyleException(String message)
        {
            super(message);
        }
    }

    protected String fetchPageContent(String url, MacroParameter macroParameter) throws IOException
    {
        try
        {
            String baseUrl = macroParameter.get("baseurl");
            if (!TextUtils.stringSet(baseUrl))
                baseUrl = url;

            String anonymousStr = TextUtils.noNull(macroParameter.get("anonymous", 1)).trim();

            if ("".equals(anonymousStr))
                anonymousStr = "false";

            GetMethod method = null;
            try
            {
                boolean useTrustedConnection = isUseTrustTokens() && !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);
                method = (GetMethod) retrieveRemoteUrl(url, useTrustedConnection);
                // Read the response body.
                String result = IOUtils.toString(method.getResponseBodyAsStream(), method.getResponseCharSet());
                try
                {
                    result = useLinkTagForStylesheetImports(result);
                } catch (MalformedStyleException e)
                {
                    // do nothing -- use original result if can't transform it
                }
                String portletData = correctBaseUrls(result, baseUrl);

                Map contextMap = MacroUtils.defaultVelocityContext();
                PageContext pageContext = WikiRendererContextKeys.getPageContext(macroParameter.getContext().getParameters());
                createContextMap(pageContext, contextMap);
                contextMap.put("portletDataHtml", portletData);
                contextMap.put("outputType", pageContext.getOutputType());
                return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraportlet.vm", contextMap);
            }
            finally
            {
                // Release the connection.
                try
                {
                    if (method != null)
                        method.releaseConnection();
                }
                catch (Exception e)
                {
                    // Don't care about this
                }
            }
        }
        catch (IOException e)
        {
            return errorContent(e.getMessage());
        }
    }

    protected void createContextMap(PageContext pageContext, Map contextMap)
    {
        contextMap.put("macroId", nextMacroId(pageContext));
        contextMap.put("generateHeader", Boolean.valueOf(generateJiraPortletHeader(pageContext)));
    }

    public String getName()
    {
        return "jiraportlet";
    }

    private String nextMacroId(RenderContext renderContext)
    {
        int macroId = 0;
        Integer id = (Integer) renderContext.getParam("nextJiraPortletMacroId");

        if (id != null)
            macroId = id.intValue();

        renderContext.addParam("nextJiraPortletMacroId", new Integer(macroId + 1));
        return "jiraportlet_"+macroId;
    }

    private boolean generateJiraPortletHeader(RenderContext renderContext)
    {
        String headerGenerated = (String) renderContext.getParam("jiraPortletHeaderGenerated");

        if (StringUtils.isEmpty(headerGenerated)) {
            renderContext.addParam("jiraPortletHeaderGenerated", "true");
            return true;
        }
        return false;
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
}