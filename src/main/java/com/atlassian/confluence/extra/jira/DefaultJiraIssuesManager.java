package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DefaultJiraIssuesManager implements JiraIssuesManager
{
    private static final Logger log = Logger.getLogger(JiraIssuesManager.class);

    private static final int MIN_JIRA_BUILD_FOR_SORTING = 328; // this isn't known to be the exact build number, but it is slightly greater than or equal to the actual number, and people shouldn't really be using the intervening versions anyway

    private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    private JiraIssuesIconMappingManager jiraIssuesIconMappingManager;

    private TrustedTokenFactory trustedTokenFactory;

    private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    private HttpRetrievalService httpRetrievalService;

    private String saxParserClass;

    public DefaultJiraIssuesManager(
            JiraIssuesSettingsManager jiraIssuesSettingsManager,
            JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager,
            JiraIssuesIconMappingManager jiraIssuesIconMappingManager,
            TrustedTokenFactory trustedTokenFactory,
            TrustedConnectionStatusBuilder trustedConnectionStatusBuilder,
            HttpRetrievalService httpRetrievalService,
            String saxParserClass)
    {
        this.jiraIssuesSettingsManager = jiraIssuesSettingsManager;
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
        this.jiraIssuesIconMappingManager = jiraIssuesIconMappingManager;
        this.trustedTokenFactory = trustedTokenFactory;
        this.trustedConnectionStatusBuilder = trustedConnectionStatusBuilder;
        this.httpRetrievalService = httpRetrievalService;
        this.saxParserClass = saxParserClass;
    }

    public Map<String, String> getColumnMap(String jiraIssuesUrl)
    {
        return jiraIssuesColumnManager.getColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl));
    }

    public void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMap)
    {
        jiraIssuesColumnManager.setColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl), columnMap);
    }

    private boolean isTargetJiraInstanceCapableOfSorting(String url, boolean useTrustedConnection) throws IOException
    {
        boolean enableSort = true;

        if (url.indexOf("tempMax=") >= 0)
        {
            url = url.replaceAll("([\\?&])tempMax=\\d+", "$1tempMax=0");
        }
        else
        {
            url += (url.indexOf("?") >= 0 ? "&" : "?") + "tempMax=0";
        }

        JiraIssuesManager.Channel channel = retrieveXML(url, useTrustedConnection);
        Element buildInfoElement = channel.getChannelElement().getChild("build-info");
        if(buildInfoElement==null) // jira is older than when the version numbers went into the xml
            enableSort = false;
        else
        {
            Element buildNumberElement = buildInfoElement.getChild("build-number");
            String buildNumber = buildNumberElement.getValue();
            try
            {
                if(Integer.parseInt(buildNumber)< MIN_JIRA_BUILD_FOR_SORTING) // if old version, no sorting
                    enableSort = false;
            }
            catch (NumberFormatException nfe)
            {
                log.warn("JIRA build number not an integer? " + buildNumber, nfe);
                enableSort = false;
            }
        }
        return enableSort;
    }

    /**
     * Checks if column sorting is supported for the target JIRA instance specified in the URL.
     * @param jiraIssuesUrl
     * The site.
     * @param useTrustedConnection
     * If <tt>true</tt> the implementation is required to figure out whether to support sorting by
     * talking to JIRA over a trusted connection. If <tt>false</tt>, the implementation should not talk to JIRA
     * for the same information over a trusted connection.
     * @return
     * Returns <tt>true</tt> or <tt>false</tt> depending on the last value specified to {@link #setSortEnabled(String, boolean)}.
     * If the method has never been called before, auto-detection for the capability is done. The result of the
     * auto-detection will then be remembered, so that the process won't be repeated for the same site.
     * @throws IOException
     * Thrown if there's an input/output error while doing the autodetection
     */
    public boolean isSortEnabled(String jiraIssuesUrl, boolean useTrustedConnection) throws IOException
    {
        String jiraIssuesUrlWithoutQueryString = jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl);
        JiraIssuesSettingsManager.Sort sort = jiraIssuesSettingsManager.getSort(jiraIssuesUrlWithoutQueryString);

        if (sort.equals(JiraIssuesSettingsManager.Sort.SORT_UNKNOWN))
        {
            boolean isTargetJiraInstanceCapableOfSorting = isTargetJiraInstanceCapableOfSorting(jiraIssuesUrl, useTrustedConnection);

            /* Remember for while if the JIRA instance supports sorting */
            setSortEnabled(jiraIssuesUrl, isTargetJiraInstanceCapableOfSorting);
            sort = jiraIssuesSettingsManager.getSort(jiraIssuesUrlWithoutQueryString);
        }
        
        return sort.equals(JiraIssuesSettingsManager.Sort.SORT_DISABLED) ? Boolean.FALSE : Boolean.TRUE;
    }

    public void setSortEnabled(final String jiraIssuesUrl, final boolean enableSort)
    {
        jiraIssuesSettingsManager.setSort(
                jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl),
                enableSort
                        ? JiraIssuesSettingsManager.Sort.SORT_ENABLED
                        : JiraIssuesSettingsManager.Sort.SORT_DISABLED
        );
    }

    private Element getChannelElement(InputStream responseStream) throws IOException
    {
        try
        {
            SAXBuilder saxBuilder = new SAXBuilder(saxParserClass);
            Document document = saxBuilder.build(responseStream);
            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        catch (JDOMException e)
        {
            log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    public Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
    {
        HttpRequest req = httpRetrievalService.getDefaultRequestFor(url);
        if (useTrustedConnection)
        {
            req.setAuthenticator(new TrustedTokenAuthenticator(trustedTokenFactory));
        }

        HttpResponse resp = httpRetrievalService.get(req);
        try
        {
            if (resp.isFailed())
            {
                // tempMax is invalid CONFJIRA-49
                if (resp.getStatusCode() == HttpServletResponse.SC_FORBIDDEN)
                {
                    throw new IllegalArgumentException(resp.getStatusMessage());
                }
                else
                {
                    log.error("Received HTTP " + resp.getStatusCode() + " from server. Error message: " + StringUtils.defaultString(resp.getStatusMessage(), "No status message"));
                    // we're not sure how to handle any other error conditions at this point
                    throw new RuntimeException(resp.getStatusMessage());
                }
            }
            Element channelElement = getChannelElement(resp.getResponse());

            TrustedConnectionStatus trustedConnectionStatus = null;
            if (useTrustedConnection)
            {
                trustedConnectionStatus = trustedConnectionStatusBuilder.getTrustedConnectionStatus(resp);
            }

            return new Channel(url, channelElement, trustedConnectionStatus);
        }
        finally
        {
            resp.finish();
        }
    }

    public Map<String, String> getIconMap(Element itemElement)
    {
        return jiraIssuesIconMappingManager.getFullIconMapping(itemElement.getChild("link").getValue());
    }
}
