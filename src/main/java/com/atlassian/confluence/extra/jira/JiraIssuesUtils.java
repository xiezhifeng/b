package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.httpclient.HttpClientHttpResponse;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utilities for JIRA Issues Macro.
 */
public class JiraIssuesUtils
{

    private static final Logger log = Logger.getLogger(JiraIssuesUtils.class);
    public static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";

    private TrustedTokenAuthenticator trustedTokenAuthenticator;
    private HttpRetrievalService httpRetrievalService;
    private JiraIconMappingManager jiraIconMappingManager;

    public void setTrustedTokenFactory(TrustedTokenFactory trustedTokenFactory)
    {
        this.trustedTokenAuthenticator = new TrustedTokenAuthenticator(trustedTokenFactory);
    }

    public void setHttpRetrievalService(HttpRetrievalService httpRetrievalService)
    {
        this.httpRetrievalService = httpRetrievalService;
    }

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    public Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
    {
        HttpRequest req = httpRetrievalService.getDefaultRequestFor(url);
        if (useTrustedConnection)
        {
            req.setAuthenticator(trustedTokenAuthenticator);
        }

        HttpResponse resp = httpRetrievalService.get(req);
        try
        {
            Element channelElement = getChannelElement(resp.getResponse());

            TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus = null;
            if (useTrustedConnection && resp instanceof HttpClientHttpResponse)
                trustedConnectionStatus = ((HttpClientHttpResponse) resp).getTrustedConnectionStatus();

            return new Channel(channelElement, trustedConnectionStatus);
        }
        finally
        {
            resp.finish();
        }
    }

    public Map prepareIconMap(Element channel)
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
