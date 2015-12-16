package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.ClientId;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.IOException;
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
     *
     * @param jiraIssuesManager       see {@link com.atlassian.confluence.extra.jira.JiraIssuesManager}
     * @param applicationLinkResolver see {@link com.atlassian.confluence.extra.jira.ApplicationLinkResolver}
     * @param jiraConnectorManager    see {@link com.atlassian.confluence.extra.jira.JiraConnectorManager}
     * @param jiraExceptionHelper     see {@link com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper}
     */
    public DefaultJiraIssueBatchService(JiraIssuesManager jiraIssuesManager, ApplicationLinkResolver applicationLinkResolver, JiraConnectorManager jiraConnectorManager, JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraIssuesManager = jiraIssuesManager;
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    /**
     * Build waiting placeholders which wait the response data from Jira
     *
     * @clientId                distinct requests from different end points
     * @param serverId          the Jira Server ID
     * @param keys              a set of issue keys
     * @param conversionContext the current ConversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue,
     * e.g.: http://jira.example.com/jira/browse/
     * @throws MacroExecutionException
     * @throws UnsupportedJiraServerException
     */
    public Map<String, Object> getPlaceHolderBatchResults(ClientId clientId, String serverId, Set<String> keys, ConversionContext conversionContext)
            throws MacroExecutionException, UnsupportedJiraServerException
    {
        ReadOnlyApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        if (appLink != null)
        {
            // make request to JIRA and build results
            Map<String, Object> resultsMap = Maps.newHashMap();
            Map<String, Element> elementMap = Maps.newHashMap();
            List<Element> entries = createPlaceHoldersList(clientId.toString(), keys);
            for (Element item : entries)
            {
                elementMap.put(item.getChild(JiraIssuesMacro.KEY).getValue(), item);
            }
            resultsMap.put(ELEMENT_MAP, elementMap);
            String jiraServerUrl = JiraUtil.normalizeUrl(appLink.getDisplayUrl()) + "/browse/";
            resultsMap.put(JIRA_SERVER_URL, jiraServerUrl);
            return resultsMap;
        }
        else
        {
            throw new MacroExecutionException(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
        }
    }

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     *
     * @param serverId          the JIRA Server ID
     * @param keys              a set of keys to be put in the KEY IN JQL
     * @param conversionContext the current ConversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.: http://jira.example.com/jira/browse/
     * @throws MacroExecutionException
     */
    public Map<String, Object> getBatchResults(String serverId, Set<String> keys, ConversionContext conversionContext) throws MacroExecutionException, UnsupportedJiraServerException
    {
        ReadOnlyApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        if (appLink != null)
        {
            // check if JIRA server version is greater than 6.0.2 (build number 6097)
            // if so, continue. Otherwise, skip this
            //When user hasn't authenticated, buildNumber == -1, it should not be unsupported jira exception
            JiraServerBean jiraServerBean = jiraConnectorManager.getJiraServer(appLink);
            if (jiraServerBean.getBuildNumber() == -1 || jiraServerBean.getBuildNumber() >= SUPPORTED_JIRA_SERVER_BUILD_NUMBER)
            {
                // make request to JIRA and build results
                Map<String, Object> resultsMap = Maps.newHashMap();
                Map<String, Element> elementMap = Maps.newHashMap();

                StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");
                for (String key : keys)
                {
                    jqlQueryBuilder.append(key).append(",");
                }
                jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length() - 1).append(")");
                JiraRequestData jiraRequestData = new JiraRequestData(jqlQueryBuilder.toString(), JiraIssuesMacro.Type.JQL);

                JiraIssuesManager.Channel channel = retrieveChannel(jiraRequestData, conversionContext, appLink);
                if (channel != null)
                {
                    Element element = channel.getChannelElement();
                    List<Element> entries = element.getChildren(JiraIssuesMacro.ITEM);
                    for (Element item : entries)
                    {
                        elementMap.put(item.getChild(JiraIssuesMacro.KEY).getValue(), item);
                    }
                    resultsMap.put(ELEMENT_MAP, elementMap);
                    String jiraServerUrl = JiraUtil.normalizeUrl(appLink.getDisplayUrl()) + "/browse/";
                    resultsMap.put(JIRA_SERVER_URL, jiraServerUrl);
                    return resultsMap;
                }
            }
            else
            {
                throw new UnsupportedJiraServerException();
            }
        }
        else
        {
            throw new MacroExecutionException(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
        }
        return Maps.newHashMap();
    }

    private List<Element> createPlaceHoldersList(String clientId, Set<String> issueKeys)
    {
        List<Element> elements = Lists.newArrayList();
        for(String key : issueKeys)
        {
            elements.add(createPlaceHolderElement(clientId, key));
        }
        return elements;
    }

    private Element createPlaceHolderElement(String clientId, String issueKey)
    {
        Element element = new Element("item");
        Element key = new Element("key");
        Element summary = new Element("summary");
        Element type = new Element("type");
        Element status = new Element("status");
        Element isPlaceholder = new Element("isPlaceholder");
        Element clientIdElement = new Element(JiraIssuesMacro.CLIENT_ID);
        clientIdElement.setText(clientId);

        key.setText(issueKey);
        // add a fake data
        type.setText("Task");

        element.addContent(key);
        element.addContent(summary);
        element.addContent(type);
        element.addContent(status);
        element.addContent(isPlaceholder);
        element.addContent(clientIdElement);
        return element;
    }

    /**
     * Send a GET request to the JIRA server
     *
     * @param jiraRequestData   the JiraRequestData instance
     * @param conversionContext the current ConversionContext
     * @param applicationLink   the Application Link to the JIRA server
     * @return the Channel instance which represents the results we get from JIRA
     * @throws MacroExecutionException
     * TODO: change to private method once we apply private method mocking in Unit Test
     */
    protected JiraIssuesManager.Channel retrieveChannel(JiraRequestData jiraRequestData, ConversionContext conversionContext, ReadOnlyApplicationLink applicationLink) throws MacroExecutionException
    {
        String requestData = jiraRequestData.getRequestData();
        JiraIssuesManager.Channel channel = null;
        String url = getXmlUrl(requestData, applicationLink);

        boolean forceAnonymous = false;
        // support rendering macros which were created without applink by legacy macro
        if (applicationLink == null)
        {
            forceAnonymous = true;
        }
        try
        {
            // The 5th parameter - useCache = false because we don't use cache here because single issue's status can be changed later
            channel = jiraIssuesManager.retrieveXMLAsChannel(url, JiraIssuesColumnManager.SINGLE_ISSUE_COLUMN_NAMES, applicationLink,
                    forceAnonymous, false);
            return channel;
        }
        catch (CredentialsRequiredException credentialsRequiredException)
        {
            try
            {
                channel = jiraIssuesManager.retrieveXMLAsChannel(url, JiraIssuesColumnManager.SINGLE_ISSUE_COLUMN_NAMES, applicationLink,
                        true, false);
                return channel;
            }
            catch (Exception e)
            {
                jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
            }

            return null; // we will send a request for each of single issues later
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

    private String getXmlUrl(String requestData, ReadOnlyApplicationLink appLink) throws MacroExecutionException
    {
        StringBuilder stringBuilder = new StringBuilder(JiraUtil.normalizeUrl(appLink.getRpcUrl()));

        stringBuilder.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                     .append(JiraUtil.MAXIMUM_ISSUES).append("&returnMax=true").append("&validateQuery=false").append("&jqlQuery=");
        stringBuilder.append(JiraUtil.utf8Encode(requestData));
        return stringBuilder.toString();
    }
}
