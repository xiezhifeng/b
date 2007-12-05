/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 15, 2004
 * Time: 3:23:53 PM
 */
package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.include.AbstractHttpRetrievalMacro;
import com.atlassian.renderer.util.UrlUtil;
import com.opensymphony.util.TextUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;

public class PortletMacro extends AbstractHttpRetrievalMacro
{
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

            HttpMethod method = null;
            try
            {
                boolean useTrustedConnection = trustedApplicationConfig.isUseTrustTokens() && !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);
                method = retrieveRemoteUrl(url, useTrustedConnection);
                // Read the response body.
                return UrlUtil.correctBaseUrls(method.getResponseBodyAsString(), baseUrl);
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
    
    public String getName()
    {
        return "jiraportlet";
    }
}