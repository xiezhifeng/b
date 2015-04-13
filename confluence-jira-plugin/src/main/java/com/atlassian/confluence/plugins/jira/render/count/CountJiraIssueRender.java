package com.atlassian.confluence.plugins.jira.render.count;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.FlexigridResponseGenerator;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CountJiraIssueRender extends JiraIssueRender {

    private static final Logger LOGGER = Logger.getLogger(CountJiraIssueRender.class);

    private static final String PLACEHOLDER_SERVLET = "/plugins/servlet/image-generator";
    private static final String XML_SEARCH_REQUEST_URI = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    private static final String COUNT = "count";
    private static final String DEFAULT_JIRA_ISSUES_COUNT = "0";

    private FlexigridResponseGenerator flexigridResponseGenerator;

    @Override
    public ImagePlaceholder getImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath)
    {
        String requestData = jiraRequestData.getRequestData();
        String url = requestData;
        ApplicationLink appLink = null;
        String totalIssues;
        try
        {
            String jql = null;
            appLink = applicationLinkResolver.resolve(jiraRequestData.getRequestType(), requestData, parameters);
            switch (jiraRequestData.getRequestType())
            {
                case JQL:
                    jql = requestData;
                    break;

                case URL:
                    if (JiraJqlHelper.isUrlFilterType(requestData))
                    {
                        jql = JiraJqlHelper.getJQLFromFilter(appLink, url, jiraIssuesManager, getI18NBean());
                    }
                    else if (requestData.matches(JiraJqlHelper.URL_JQL_REGEX))
                    {
                        jql = JiraJqlHelper.getJQLFromJQLURL(url);
                    }
                    break;
            }

            if (jql != null)
            {
                url = appLink.getRpcUrl() + XML_SEARCH_REQUEST_URI + "?jqlQuery=" + JiraUtil.utf8Encode(jql) + "&tempMax=0&returnMax=true";
            }

            boolean forceAnonymous = parameters.get("anonymous") != null && Boolean.parseBoolean(parameters.get("anonymous"));
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), appLink, forceAnonymous, false);
            totalIssues = flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);
        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Continues request by anonymous user");
            totalIssues = getTotalIssuesByAnonymous(url, appLink);
        }
        catch (Exception e)
        {
            LOGGER.error("Error generate count macro placeholder: " + e.getMessage(), e);
            totalIssues = "-1";
        }
        return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=" + totalIssues, null, false);
    }

    private String getTotalIssuesByAnonymous(String url, ApplicationLink appLink)
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, new ArrayList<String>(), appLink, false, false);
            return flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);
        }
        catch (Exception e)
        {
            LOGGER.error("Can't retrive issues by anonymous");
            return "-1";
        }
    }

    @Override
    public String getMobileTemplate(Map<String, Object> contextMap)
    {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileShowCountJiraissues.vm", contextMap);
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap)
    {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticShowCountJiraissues.vm", contextMap);
    }

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink, forceAnonymous, useCache);
            Element element = channel.getChannelElement();
            Element totalItemsElement = element.getChild("issue");
            String count = totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : "" + element.getChildren("item").size();

            contextMap.put(COUNT, count);
        }
        catch (CredentialsRequiredException e)
        {
            contextMap.put(COUNT, getCountIssuesWithAnonymous(url, columnNames, appLink, forceAnonymous, useCache));
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            contextMap.put(COUNT, DEFAULT_JIRA_ISSUES_COUNT);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private String getCountIssuesWithAnonymous(String url, List<String> columnNames, ApplicationLink appLink, boolean forceAnonymous, boolean useCache) throws MacroExecutionException {
        try {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames, appLink, forceAnonymous, useCache);
            Element element = channel.getChannelElement();
            Element totalItemsElement = element.getChild("issue");
            return totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : "" + element.getChildren("item").size();
        }
        catch (Exception e)
        {
            LOGGER.info("Can not retrieve total issues by anonymous");
            return DEFAULT_JIRA_ISSUES_COUNT;
        }
    }

    public void setFlexigridResponseGenerator(FlexigridResponseGenerator flexigridResponseGenerator) {
        this.flexigridResponseGenerator = flexigridResponseGenerator;
    }
}
