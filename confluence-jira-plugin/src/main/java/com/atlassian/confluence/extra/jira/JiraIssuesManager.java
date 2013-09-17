package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.sal.api.net.ResponseException;

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

    public Channel retrieveXMLAsChannel(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException;

    public Channel retrieveXMLAsChannelByAnonymous(final String url, List<String> columns, ApplicationLink applink,
            boolean forceAnonymous, boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException;

    public String retrieveXMLAsString(final String url, List<String> columns, ApplicationLink applink,
            boolean forceAnonymous, boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException;

    public String retrieveJQLFromFilter(final String filterId, ApplicationLink appLink) throws ResponseException;

    public String checkFilterId(final String filterId, ApplicationLink appLink) throws ResponseException;

    /**
     * Create jira issues from the list of jira issue bean
     * 
     * @param jiraIssueBeans
     * @param appLink
     * @return List<JiraIssueBean> list of jira issue beans
     * @throws CredentialsRequiredException
     */
    public List<JiraIssueBean> createIssues(List<JiraIssueBean> jiraIssueBeans, ApplicationLink appLink)
            throws CredentialsRequiredException;

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
