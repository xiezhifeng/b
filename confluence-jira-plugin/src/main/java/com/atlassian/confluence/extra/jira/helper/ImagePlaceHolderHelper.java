package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.pages.thumbnail.Dimensions;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

public class ImagePlaceHolderHelper
{
    private static final Logger LOGGER = Logger.getLogger(ImagePlaceHolderHelper.class);
    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";
    private static final String JIRA_ISSUES_RESOURCE_PATH = "jiraissues-xhtml";
    private static final String JIRA_ISSUES_SINGLE_MACRO_TEMPLATE = "{jiraissues:key=%s}";
    private static final String JIRA_SINGLE_MACRO_TEMPLATE = "{jira:key=%s}";
    private static final String JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE = "/plugins/servlet/confluence/placeholder/macro?definition=%s&locale=%s";
    private static final String PLACEHOLDER_SERVLET = "/plugins/servlet/image-generator";
    private static final String XML_SEARCH_REQUEST_URI = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";

    private LocaleManager localeManager;
    private ApplicationLinkResolver applicationLinkResolver;
    private JiraIssuesManager jiraIssuesManager;
    private FlexigridResponseGenerator flexigridResponseGenerator;
    private I18NBeanFactory i18NBeanFactory;

    public ImagePlaceHolderHelper(JiraIssuesManager jiraIssuesManager, LocaleManager localeManager, I18NBeanFactory i18NBeanFactory,
                                  ApplicationLinkResolver applicationLinkResolver, FlexigridResponseGenerator flexigridResponseGenerator)
    {
        this.localeManager = localeManager;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraIssuesManager = jiraIssuesManager;
        this.flexigridResponseGenerator = flexigridResponseGenerator;
    }

    public I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }

    /**
     * @param jiraRequestData
     * @param parameters
     * @param resourcePath
     * @return Jira Issue Macro Image Placeholder
     */
    public ImagePlaceholder getJiraMacroImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath)
    {
        String requestData = jiraRequestData.getRequestData();
        JiraIssuesMacro.Type requestType = jiraRequestData.getRequestType();
        JiraIssuesMacro.JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, jiraRequestData.getRequestType(), requestData);

        switch (issuesType)
        {
            case SINGLE:
                String key = requestData;
                if (requestType == JiraIssuesMacro.Type.URL)
                {
                    key = JiraJqlHelper.getKeyFromURL(requestData);
                }
                return getSingleImagePlaceHolder(key, resourcePath);

            case COUNT:
                return getCountImagePlaceHolder(parameters, requestType, requestData);

            case TABLE:
                return new DefaultImagePlaceholder(JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH, (Dimensions) null, false);
        }

        return null;
    }

    /**
     * Get Image Placeholder of single issue
     * @param key
     * @param resourcePath
     * @return Jira Single Issue Macro Image Placeholder
     */
    private ImagePlaceholder getSingleImagePlaceHolder(String key, String resourcePath)
    {
        String macro = resourcePath.contains(JIRA_ISSUES_RESOURCE_PATH) ?
                String.format(JIRA_ISSUES_SINGLE_MACRO_TEMPLATE, key) : String.format(JIRA_SINGLE_MACRO_TEMPLATE, key);
        byte[] encoded = Base64.encodeBase64(macro.getBytes());
        String locale = localeManager.getSiteDefaultLocale().toString();
        String placeHolderUrl = String.format(JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE, new String(encoded), locale);

        return new DefaultImagePlaceholder(placeHolderUrl, (Dimensions) null, false);
    }

    /**
     * Get Image Placeholder of count issue
     * @param params
     * @param requestType
     * @param requestData
     * @return Jira Count Issue Macro Image Placeholder
     */
    private ImagePlaceholder getCountImagePlaceHolder(Map<String, String> params, JiraIssuesMacro.Type requestType, String requestData)
    {
        String url = requestData;
        ApplicationLink appLink = null;
        String totalIssues;
        try
        {
            String jql = null;
            appLink = applicationLinkResolver.resolve(requestType, requestData, params);
            switch (requestType)
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

            boolean forceAnonymous = params.get("anonymous") != null && Boolean.parseBoolean(params.get("anonymous"));
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
        return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=" + totalIssues, (Dimensions) null, false);
    }

    private String getTotalIssuesByAnonymous(String url, ApplicationLink appLink)
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                    url, new ArrayList<String>(), appLink, false, false);
            return flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);
        }
        catch (Exception e)
        {
            LOGGER.info("Can't retrive issues by anonymous");
            return "-1";
        }
    }
}
