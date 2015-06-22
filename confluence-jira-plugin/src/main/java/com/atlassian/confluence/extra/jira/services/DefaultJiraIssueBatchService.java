package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
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
import org.jdom.output.XMLOutputter;

import java.util.ArrayList;
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
        ApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        if (appLink != null)
        {
            // check if JIRA server version is greater than 6.0.2 (build number 6097)
            // if so, continue. Otherwise, skip this
//            JiraServerBean jiraServerBean = jiraConnectorManager.getJiraServer(appLink);
//            if (jiraServerBean.getBuildNumber() >= SUPPORTED_JIRA_SERVER_BUILD_NUMBER)
            {
                // make request to JIRA and build results
                Map<String, Object> resultsMap = Maps.newHashMap();
                Map<String, Element> elementMap = Maps.newHashMap();

//                StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");
//                for (String key : keys)
//                {
//                    jqlQueryBuilder.append(key).append(",");
//                }
//                jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length() - 1).append(")");
//                JiraRequestData jiraRequestData = new JiraRequestData(jqlQueryBuilder.toString(), JiraIssuesMacro.Type.JQL);
//
//                JiraIssuesManager.Channel channel = retrieveChannel(jiraRequestData, conversionContext, appLink);
//                if (channel != null)
//                {
//                    Element element = channel.getChannelElement();
//                    List<Element> entries = element.getChildren(JiraIssuesMacro.ITEM);
                    List<Element> entries = createPlaceHolderElement(keys);
                    for (Element item : entries)
                    {
                        XMLOutputter xmlOutputter = new XMLOutputter();
                        String s = xmlOutputter.outputString(item);
                        elementMap.put(item.getChild(JiraIssuesMacro.KEY).getValue(), item);
                    }
                    resultsMap.put(ELEMENT_MAP, elementMap);
                    String jiraServerUrl = JiraUtil.normalizeUrl(appLink.getDisplayUrl()) + "/browse/";
                    resultsMap.put(JIRA_SERVER_URL, jiraServerUrl);
                    return resultsMap;
//                }
            }
//            else
//            {
//                throw new UnsupportedJiraServerException();
//            }
        }
        else
        {
            LOGGER.debug(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
            throw new MacroExecutionException(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
        }
//        return null;
    }

    private List<Element> createPlaceHolderElement(Set<String> issueKeys)
    {
        List<Element> elements = new ArrayList<Element>();
        for(String key: issueKeys)
        {
            elements.add(createPlaceHolderElement(key));
        }
        return elements;
    }

    private Element createPlaceHolderElement(String issueKey)
    {
        Element element = new Element("item");
        Element key = new Element("key");
        Element link = new Element("link");
        Element summary = new Element("summary");
        Element type = new Element("type");
        Element status = new Element("status");
        Element resolution = new Element("resolution");

        key.setText(issueKey).setAttribute("id", "10001");
        link.setText("http://localhost:11990/jira/browse/TEST-2");
        summary.setText("Loading...");
        type.setText("Task").setAttribute("id", "3").setAttribute("iconUrl", "https://www.appmybizaccount.gov.on.ca/sodp/osb/public/images/icon/loading.gif");
        status.setText("TODO").setAttribute("id", "1000").setAttribute("iconUrl", "http://localhost:11990/jira/images/icons/statuses/open.png");
        resolution.setText("Unresolved").setAttribute("id", "-1");

        element.addContent(key);
        element.addContent(link);
        element.addContent(summary);
        element.addContent(type);
        element.addContent(status);
        element.addContent(resolution);
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
    protected JiraIssuesManager.Channel retrieveChannel(JiraRequestData jiraRequestData, ConversionContext conversionContext, ApplicationLink applicationLink) throws MacroExecutionException
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

    private String getXmlUrl(String requestData, ApplicationLink appLink) throws MacroExecutionException
    {
        StringBuilder stringBuilder = new StringBuilder(JiraUtil.normalizeUrl(appLink.getRpcUrl()));

        stringBuilder.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                     .append(JiraUtil.MAXIMUM_ISSUES).append("&returnMax=true").append("&validateQuery=false").append("&jqlQuery=");
        stringBuilder.append(JiraUtil.utf8Encode(requestData));
        return stringBuilder.toString();
    }
}
