package com.atlassian.confluence.extra.jira;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.JiraIconMappingManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Utilities for JIRA Issues Macro.
 */
public class JiraIssuesUtils
{

    private static final Logger log = Logger.getLogger(JiraIssuesUtils.class);
    public static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";

    public static Channel retrieveXML(String url, boolean useTrustedConnection,
        TrustedTokenAuthenticator trustedTokenAuthenticator) throws IOException
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = getMethod(url, useTrustedConnection, httpClient, trustedTokenAuthenticator);

        httpClient.executeMethod(method);
        InputStream xmlStream = method.getResponseBodyAsStream();
        Element channelElement = getChannelElement(xmlStream);

        TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus = null;
        if (useTrustedConnection)
            trustedConnectionStatus = getTrustedConnectionStatusFromMethod(trustedTokenAuthenticator, method);

        return new Channel(channelElement, trustedConnectionStatus);

        // TODO: check that this is really not needed b/c an autocloseinputstream is used
//        finally
//        {
//            method.releaseConnection();
//        }
    }

    public static Map prepareIconMap(Element channel, JiraIconMappingManager jiraIconMappingManager)
    {
        String link = channel.getChild("link").getValue();
        // In pre 3.7 JIRA, the link is just http://domain/context, in 3.7 and later it is the full query URL,
        // which looks like http://domain/context/secure/IssueNaviagtor...
        int index = link.indexOf("/secure/IssueNavigator");
        if (index != -1)
            link = link.substring(0, index);

        String imagesRoot = link + "/images/icons/";
        Map result = new HashMap();

        for (Iterator iterator = jiraIconMappingManager.getIconMappings().entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            String icon = (String) entry.getValue();
            if (icon.startsWith("http://") || icon.startsWith("https://"))
                result.put(entry.getKey(), icon);
            else
                result.put(GeneralUtil.escapeXml((String) entry.getKey()), imagesRoot + icon);
        }

        return result;
    }

    private static HttpMethod getMethod(String url, boolean useTrustedConnection, HttpClient client,
        TrustedTokenAuthenticator trustedTokenAuthenticator)
    {
        return (useTrustedConnection ? trustedTokenAuthenticator.makeMethod(client, url) : new GetMethod(url));
    }

    /**
     * Query the status of a trusted connection
     *
     * @param method An executed HttpClient method
     * @return the response status of a trusted connection request or null if the method doesn't use a trusted
     *         connection
     */
    private static TrustedTokenAuthenticator.TrustedConnectionStatus getTrustedConnectionStatusFromMethod(
        TrustedTokenAuthenticator trustedTokenAuthenticator, HttpMethod method)
    {
        return trustedTokenAuthenticator.getTrustedConnectionStatus(method);
    }

    private static Element getChannelElement(InputStream responseStream) throws IOException
    {
        try
        {
            SAXBuilder saxBuilder = new SAXBuilder(SAX_PARSER_CLASS);
            Document document = saxBuilder.build(responseStream);
            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        catch (JDOMException e)
        {
            log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    /*
    * fetchChannel needs to return its result plus a trusted connection status. This is a value class to allow this.
    */
    public final static class Channel
    {
        private final Element element;
        private final TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus;

        protected Channel(Element element, TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus)
        {
            this.element = element;
            this.trustedConnectionStatus = trustedConnectionStatus;
        }

        public Element getElement()
        {
            return element;
        }

        public TrustedTokenAuthenticator.TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return trustedConnectionStatus;
        }

        public boolean isTrustedConnection()
        {
            return trustedConnectionStatus != null;
        }
    }
}
