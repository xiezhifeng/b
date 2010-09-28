package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import org.jdom.Element;

import java.io.IOException;
import java.util.Map;

/**
 * The facade for most <tt>JiraXXXManager</tt> classes. Implementations can choose to
 * perform any pre/post processing before/after handing control to the appropriate managers.
 */
public interface JiraIssuesManager
{
    /**
     * Gets a site specific column mapping from a {@link com.atlassian.confluence.extra.jira.JiraIssuesIconMappingManager}.
     * @param jiraIssuesUrl
     * The site.
     * @return
     * A {@link java.util.Map} representing the column mapping.
     */
    Map<String, String> getColumnMap(String jiraIssuesUrl);

    /**
     * Sets a column mapping for a site.
     * @param jiraIssuesUrl
     * The site to set for.
     * @param columnMap
     * A {@link java.util.Map} representing the column mapping.
     */
    void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMap);

    /**
     * Checks if column sorting is supported for the target JIRA instance specified in the URL.
     *
     * @param jiraIssuesUrl JIRA Issues URL.
     * @param useTrustedConnection If <tt>true</tt> the implementation is required to figure out whether to support sorting by
     * talking to JIRA over a trusted connection. If <tt>false</tt>, the implementation should not talk to JIRA
     * for the same information over a trusted connection.
     * @return <tt>true</tt> or <tt>false</tt> depending on the last value specified to {@link #setSortEnabled(String, boolean)}.
     * If the method has never been called before, auto-detection for the capability is done. The result of the
     * auto-detection will then be remembered, so that the process won't be repeated for the same site.
     * @throws IOException if there's an input/output error while detecting if sort is enabled or not.
     */
    boolean isSortEnabled(String jiraIssuesUrl, boolean useTrustedConnection) throws IOException;

    /**
     * Remember if sorting is enabled for a particular site.
     * @param jiraIssuesUrl
     * The site
     * @param enableSort
     * If <tt>true</tt>, implementations must remember that sorting is enabled for the site. If <tt>false</tt>
     * implementations must remember that sorting is disabled for the site. 
     */
    void setSortEnabled(final String jiraIssuesUrl, final boolean enableSort);

    /**
     * Gets the JIRA issues response
     * @param url
     * The JIRA issues URL.
     * @param useTrustedConnection
     * If <tt>true</tt>, the data must be requested over a trusted connection. If <tt>false</tt>,
     * the data must not be requested over a trusted connection.
     * @return
     * A {@link com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel} representing the read response.
     * @throws IOException
     */
    Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException;

    /**
     * Gets the <em>full</em> icon mapping based on the element that represents an &lt;item/&gt; tag.
     * @param itemElement
     * The &lt;item/&gt; tag.
     * @return
     * The full icon mapping.
     * @see
     * {@link com.atlassian.confluence.extra.jira.JiraIssuesIconMappingManager#getFullIconMapping(String)} 
     */
    Map<String, String> getIconMap(Element itemElement);


    /*
    * fetchChannel needs to return its result plus a trusted connection status. This is a value class to allow this.
    */
    static class Channel
    {
        private final String sourceUrl;

        private final Element channelElement;

        private final TrustedConnectionStatus trustedConnectionStatus;

        protected Channel(String sourceUrl, Element channelElement, TrustedConnectionStatus trustedConnectionStatus)
        {
            this.sourceUrl = sourceUrl;
            this.channelElement = channelElement;
            this.trustedConnectionStatus = trustedConnectionStatus;
        }

        public String getSourceUrl()
        {
            return sourceUrl;
        }

        public Element getChannelElement()
        {
            return channelElement;
        }

        public TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return trustedConnectionStatus;
        }

        public boolean isTrustedConnection()
        {
            return trustedConnectionStatus != null;
        }
    }
}
