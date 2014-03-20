package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraIssuesColumnManager;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.ExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
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
    private static final String KEY = "key";
    private static final String SERVER_ID = "serverId";
    private static final String ITEM = "item";

    private JiraIssuesManager jiraIssuesManager;
    private ApplicationLinkResolver applicationLinkResolver;

    private ExceptionHelper exceptionHelper;

    public void setExceptionHelper(ExceptionHelper exceptionHelper) {
        this.exceptionHelper = exceptionHelper;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setApplicationLinkResolver(ApplicationLinkResolver applicationLinkResolver)
    {
        this.applicationLinkResolver = applicationLinkResolver;
    }

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     * @param serverId the JIRA Server ID
     * @param keys a set of keys to be put in the KEY IN JQL
     * @param conversionContext the current ConversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.: http://jira.example.com/browse/
     * @throws MacroExecutionException
     */
    public Map<String, Object> getBatchResults(String serverId, Set<String> keys, ConversionContext conversionContext) throws MacroExecutionException
    {
        // make request to JIRA and build results
        Map<String, Object> map = Maps.newHashMap();
        Map<String, Element> elementMap = Maps.newHashMap();

        StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");
        for (String key : keys)
        {
            jqlQueryBuilder.append(key + ",");
        }
        jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length()-1).append(")");
        JiraRequestData jiraRequestData = new JiraRequestData(jqlQueryBuilder.toString(), JiraIssuesMacro.Type.JQL);

        JiraIssuesManager.Channel channel = retrieveChannel(serverId, jiraRequestData, conversionContext);
        if (channel != null)
        {
            Element element = channel.getChannelElement();
            List<Element> entries = element.getChildren(ITEM);
            for (Element item: entries)
            {
                elementMap.put(item.getChild(KEY).getValue(), item);
            }
            map.put(ELEMENT_MAP, elementMap);
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
            map.put(JIRA_SERVER_URL, jiraServerUrl);
        }
        return map;
    }

    /**
     * Send a GET request to the JIRA server
     * @param serverId the JIRA Server ID
     * @param jiraRequestData an JiraRequestData instance
     * @param conversionContext the current ConversionContext
     * @return the Channel instance which represents the results we get from JIRA
     * @throws MacroExecutionException
     */
    private JiraIssuesManager.Channel retrieveChannel(String serverId, JiraRequestData jiraRequestData, ConversionContext conversionContext) throws MacroExecutionException
    {
        String requestData = jiraRequestData.getRequestData();
        ApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        try
        {
            if (appLink != null)
            {
                String url = getXmlUrl(requestData, appLink);
                try
                {
                    // The 4rd parameter - forceAnonymous = false because we don't force anonymous
                    // The 5th parameter - useCache = false because we don't use cache here because single issue's status can be changed later
                    JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, JiraIssuesColumnManager.SINGLE_ISSUE_COLUMN_NAMES, appLink,
                            false, false);
                    return channel;
                }
                catch (CredentialsRequiredException e)
                {
                    LOGGER.debug("CredentialsRequiredException: " +  e.getMessage());
                    exceptionHelper.throwMacroExecutionException(e, conversionContext);
                }
                catch (MalformedRequestException e)
                {
                    LOGGER.debug("MalformedRequestException: " + e.getMessage());
                    exceptionHelper.throwMacroExecutionException(e, conversionContext);
                }
                catch (Exception e)
                {
                    LOGGER.debug("Exception: " + e.getMessage());
                    exceptionHelper.throwMacroExecutionException(e, conversionContext);
                }
            }
            else
            {
                LOGGER.debug(exceptionHelper.getText("jiraissues.error.noappLinks"));
                throw new MacroExecutionException(exceptionHelper.getText("jiraissues.error.noappLinks"));
            }
        }
        catch (MacroExecutionException e)
        {
            throw new MacroExecutionException(e);
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
