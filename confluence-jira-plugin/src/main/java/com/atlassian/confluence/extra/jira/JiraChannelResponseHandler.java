package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;

public class JiraChannelResponseHandler implements JiraResponseHandler
{

    private static final Logger log = Logger.getLogger(JiraChannelResponseHandler.class);

    Channel responseChannel;
    private String url;

    public JiraChannelResponseHandler(String url)
    {
        this.url = url;
    }

    public Channel getResponseChannel()
    {
        return responseChannel;
    }

    public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException
    {
        responseChannel = new Channel(url,getChannelElement(in), trustedConnectionStatus);
    }
    
    Element getChannelElement(InputStream responseStream) throws IOException
    {
        try
        {
            SAXBuilder saxBuilder = SAXBuilderFactory.createSAXBuilder();
            Document document = saxBuilder.build(responseStream);
            Element root = document.getRootElement();
            if (root != null)
            {
                return root.getChild("channel");
            }
            return null;
        }
        catch (JDOMException e)
        {
            log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage());
            throw new IOException(e.getMessage());
        } finally 
        {
            IOUtils.closeQuietly(responseStream);
        }
    }
}