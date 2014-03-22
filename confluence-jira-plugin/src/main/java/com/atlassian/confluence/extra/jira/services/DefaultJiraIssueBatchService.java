package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.JiraIssuesColumnManager;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultJiraIssueBatchService implements JiraIssueBatchService
{
    private static final Logger LOGGER = Logger.getLogger(DefaultJiraIssueBatchService.class);

    private final JiraIssuesManager jiraIssuesManager;
    private final ApplicationLinkResolver applicationLinkResolver;
    private final JiraConnectorManager jiraConnectorManager;
    private final JiraExceptionHelper jiraExceptionHelper;

    /**
     * Default constructor
     * @param jiraIssuesManager
     * @param applicationLinkResolver
     * @param jiraConnectorManager
     * @param jiraExceptionHelper
     */
    public DefaultJiraIssueBatchService(JiraIssuesManager jiraIssuesManager, ApplicationLinkResolver applicationLinkResolver, JiraConnectorManager jiraConnectorManager, JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraIssuesManager = jiraIssuesManager;
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     *
     * @param serverId          the JIRA Server ID
     * @param keys              a set of keys to be put in the KEY IN JQL
     * @param conversionContext the current ConversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.: http://jira.example.com/browse/
     * @throws MacroExecutionException
     */
    public Map<String, Object> getBatchResults(String serverId, Set<String> keys, ConversionContext conversionContext) throws MacroExecutionException, UnsupportedJiraServerException {
        ApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        if (appLink != null) {
            // check if JIRA server version is greater than 6.0.2 (build number 6097)
            // if so, continue. Otherwise, skip this
            JiraServerBean jiraServerBean = jiraConnectorManager.getJiraServer(appLink);
            if (jiraServerBean.getBuildNumber() >= SUPPORTED_JIRA_SERVER_BUILD_NUMBER) {
                // make request to JIRA and build results
                Map<String, Object> resultsMap = Maps.newHashMap();
                Map<String, Element> elementMap = Maps.newHashMap();

                StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");
                for (String key : keys)
                {
                    jqlQueryBuilder.append(key + ",");
                }
                jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length() - 1).append(")");
                JiraRequestData jiraRequestData = new JiraRequestData(jqlQueryBuilder.toString(), JiraIssuesMacro.Type.JQL);

                JiraIssuesManager.Channel channel = retrieveChannel(serverId, jiraRequestData, conversionContext, appLink);
                if (channel != null)
                {
                    Element element = channel.getChannelElement();
                    List<Element> entries = element.getChildren(JiraIssuesMacro.ITEM);
                    for (Element item : entries)
                    {
                        elementMap.put(item.getChild(JiraIssuesMacro.KEY).getValue(), item);
                    }
                    resultsMap.put(ELEMENT_MAP, elementMap);
                    URL sourceUrl = null;
                    try
                    {
                        sourceUrl = new URL(channel.getSourceUrl());
                    }
                    catch (MalformedURLException e)
                    {
                        throw new MacroExecutionException(e.getCause());
                    }
                    String jiraServerUrl = sourceUrl.getProtocol() + "://" + sourceUrl.getAuthority() + "/browse/";
                    resultsMap.put(JIRA_SERVER_URL, jiraServerUrl);
                    return resultsMap;
                }
            }
            else {
                throw new UnsupportedJiraServerException();
            }
        }
        else
        {
            LOGGER.debug(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
            throw new MacroExecutionException(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
        }
        return null;
    }

    /**
     * Send a GET request to the JIRA server
     *
     *
     * @param serverId          the JIRA Server ID
     * @param jiraRequestData   an JiraRequestData instance
     * @param conversionContext the current ConversionContext
     * @param appLink
     * @return the Channel instance which represents the results we get from JIRA
     * @throws MacroExecutionException
     */
    private JiraIssuesManager.Channel retrieveChannel(String serverId, JiraRequestData jiraRequestData, ConversionContext conversionContext, ApplicationLink appLink) throws MacroExecutionException
    {
        String requestData = jiraRequestData.getRequestData();
        JiraIssuesManager.Channel channel = null;
        String url = getXmlUrl(requestData, appLink);

        boolean forceAnonymous = false;
        // support rendering macros which were created without applink by legacy macro
        if (appLink == null)
        {
            forceAnonymous = true;
        }
        try
        {
            // The 5th parameter - useCache = false because we don't use cache here because single issue's status can be changed later
            channel = jiraIssuesManager.retrieveXMLAsChannel(url, JiraIssuesColumnManager.SINGLE_ISSUE_COLUMN_NAMES, appLink,
                    forceAnonymous, false);
            return channel;
        }
        catch (CredentialsRequiredException credentialsRequiredException)
        {
            try
            {
                channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                        url, JiraIssuesMacro.DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, appLink, forceAnonymous, false);
                return channel;
            }
            catch (Exception e)
            {
                jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
            }
        }
        catch (MalformedRequestException e)
        {
            LOGGER.debug("MalformedRequestException: " + e.getMessage());
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        catch (Exception e)
        {
            LOGGER.debug("Exception: " + e.getMessage());
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        return null;
    }

    private String getXmlUrl(String requestData, ApplicationLink appLink) throws MacroExecutionException
    {
        StringBuilder sf = new StringBuilder(JiraUtil.normalizeUrl(appLink.getRpcUrl()));

        sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
          .append(JiraUtil.MAXIMUM_ISSUES).append("&returnMax=true").append("&validateQuery=false").append("&jqlQuery=");
        sf.append(JiraUtil.utf8Encode(requestData));
        return sf.toString();
    }
}
