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
    protected String getHtml(MacroParameter macroParameter) throws IllegalArgumentException, IOException
    {
        String url = TextUtils.noNull(macroParameter.get("url", 0)).trim();
        url = cleanUrlParentheses(url);
        return fetchPageContent(url, macroParameter);
    }

    protected String fetchPageContent(String url, MacroParameter macroParameter) throws IOException
    {
        try
        {
            String baseUrl = macroParameter.get("baseurl");
            if (!TextUtils.stringSet(baseUrl))
                baseUrl = url;

            HttpMethod method = retrieveRemoteUrl(url);

            // Read the response body.
            String resultHtml = method.getResponseBodyAsString();

            // Release the connection.
            method.releaseConnection();

            return UrlUtil.correctBaseUrls(resultHtml, baseUrl);
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