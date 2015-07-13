package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.sal.api.net.ResponseException;
import org.jdom.Element;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;

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

    /**
     * Execute JQL query base on application link, the form of JQL should contain "jql" prefix
     * @param jqlQuery jql string, the form should be look like: "jql=type=epic&startAt=1"
     * @param applicationLink
     * @return String with JSON format.
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    public String executeJqlQuery(String jqlQuery, ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException;
    /**
     * Create jira issues from the list of jira issue bean
     * 
     * @param jiraIssueBeans
     * @param appLink
     * @return List<JiraIssueBean> list of jira issue beans
     * @throws CredentialsRequiredException
     */
    public List<JiraIssueBean> createIssues(List<JiraIssueBean> jiraIssueBeans, ApplicationLink appLink)
            throws CredentialsRequiredException, ResponseException;

    /*
     * fetchChannel needs to return its result plus a trusted connection status. This is a value class to allow this.
     */
    static class Channel implements Serializable
    {
        private static final long serialVersionUID = -6869013860734942094L;

        private final String sourceUrl;

        private final Supplier<Element> elementSupplier;

        private final TrustedConnectionStatus trustedConnectionStatus;

        protected Channel(final String sourceUrl, final Element channelElement, final TrustedConnectionStatus trustedConnectionStatus)
        {
            this.sourceUrl = sourceUrl;
            this.elementSupplier = new Supplier<Element>()
            {
                @Override
                public Element get()
                {
                    return channelElement;
                }
            };
            this.trustedConnectionStatus = trustedConnectionStatus;
        }



        protected Channel(final String sourceUrl, final byte[] bytes, final TrustedConnectionStatus
                trustedConnectionStatus)
        {
            this.sourceUrl = sourceUrl;
            this.elementSupplier = new Supplier<Element>()
            {
                final byte[] compressedBytes = compress(bytes);
                @Override
                public Element get()
                {
                    try
                    {
                        return JiraChannelResponseHandler.getChannelElement(
                                new ByteArrayInputStream(uncompress(compressedBytes)));
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
            this.trustedConnectionStatus = trustedConnectionStatus;
        }

        public String getSourceUrl()
        {
            return sourceUrl;
        }

        public Element getChannelElement()
        {
            return elementSupplier.get();
        }

        public TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return trustedConnectionStatus;
        }

        public boolean isTrustedConnection()
        {
            return trustedConnectionStatus != null;
        }

        static byte[] compress(byte[] bytes)
        {
            try
            {
                return Snappy.compress(bytes);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        static byte[] uncompress(byte[] bytes)
        {
            try
            {
                return Snappy.uncompress(bytes);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
