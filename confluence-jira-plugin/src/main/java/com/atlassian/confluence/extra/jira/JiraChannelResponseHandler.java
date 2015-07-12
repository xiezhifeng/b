package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class JiraChannelResponseHandler implements JiraResponseHandler, Serializable
{

    private static final long serialVersionUID = -4371360987898346958L;

    private static final Logger log = Logger.getLogger(JiraChannelResponseHandler.class);

    private Channel responseChannel;
    private final String url;

    public JiraChannelResponseHandler(final String url)
    {
        this.url = url;
    }

    public Channel getResponseChannel()
    {
        return this.responseChannel;
    }

    @Override
    public void handleJiraResponse(final InputStream in, final TrustedConnectionStatus trustedConnectionStatus) throws IOException
    {
        this.responseChannel = new Channel(this.url, in, trustedConnectionStatus);
    }

    @SuppressWarnings("static-method")
    static Element getChannelElement(final InputStream responseStream) throws IOException
    {
        try
        {
            final SAXBuilder saxBuilder = SAXBuilderFactory.createSAXBuilder();
            final Document document = saxBuilder.build(responseStream);
            final Element root = document.getRootElement();
            if (root != null)
            {
                return root.getChild("channel");
            }
            return null;
        }
        catch (final JDOMException e)
        {
            log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        finally
        {
            IOUtils.closeQuietly(responseStream);
        }
    }
}