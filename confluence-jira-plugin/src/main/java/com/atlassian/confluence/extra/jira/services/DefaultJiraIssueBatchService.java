package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultJiraIssueBatchService implements JiraIssueBatchService
{
    private static final String KEY_PARAM = "key";
    private static final String JQL_QUERY = "jqlQuery";

    private JiraIssuesManager jiraIssuesManager;
    private ApplicationLinkResolver applicationLinkResolver;
    private I18NBeanFactory i18NBeanFactory;
    private LocaleManager localeManager;
    private JiraCacheManager jiraCacheManager;

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory) {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public void setLocaleManager(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }

    public void setJiraCacheManager(JiraCacheManager jiraCacheManager) {
        this.jiraCacheManager = jiraCacheManager;
    }


    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager) {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setApplicationLinkResolver(ApplicationLinkResolver applicationLinkResolver) {
        this.applicationLinkResolver = applicationLinkResolver;
    }

    public Map<String, Element> getBatchResults(Map<String, String> parameters, Set<String> keys) {
        // make request to JIRA and build results
        Map<String, Element> results = Maps.newHashMap();

        StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");

        for (String key : keys) {
            jqlQueryBuilder.append(key + ",");
        }
        jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length()-1).append(")");
        parameters.put(JQL_QUERY, jqlQueryBuilder.toString());

        JiraIssuesManager.Channel channel = retrieveChannel(parameters);
        if (channel != null)
        {
            Element element = channel.getChannelElement();
            List<Element> entries = element.getChildren("item");
            for (Element item: entries)
            {
                    results.put(item.getChild("key").getValue(), item);
                    //item.getChild("summary").getValue();
            }
        }
        return results;
    }

    private JiraIssuesManager.Channel retrieveChannel(Map<String, String> parameters)
    {
        JiraRequestData jiraRequestData = parseRequestData(parameters);
        String requestData = jiraRequestData.getRequestData();
        JiraIssuesMacro.Type requestType = jiraRequestData.getRequestType();
        ApplicationLink appLink = null;
        try
        {
            appLink = applicationLinkResolver.resolve(requestType, requestData, parameters);
        }
        catch (TypeNotInstalledException tne)
        {
            //throwMacroExecutionException(tne, conversionContext);
        }

        try
        {
            Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
            //parameters.put(TOKEN_TYPE_PARAM, TokenType.INLINE.name());
            List<String> columnNames = new ArrayList<String>();
            columnNames.add("type");
            columnNames.add("summary");
            columnNames.add("status");
            columnNames.add("resolution");
            columnNames.add("statusCategory");

            String url = null;
            if (appLink != null)
            {
                url = getXmlUrl(requestData, requestType, appLink);
            }
            else
            {
                throw new MacroExecutionException(getText("jiraissues.error.noappLinks"));
            }

            boolean userAuthenticated = AuthenticatedUserThreadLocal.get() != null;
            boolean forceAnonymous = false;

            //boolean clearCache = getBooleanProperty(conversionContext.getProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE));
            boolean clearCache = false;
            boolean useCache = false;
            try
            {
                if (clearCache)
                {
                    jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, false);
                }

                JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink,
                        forceAnonymous, useCache);
                return channel;
            }
            catch (CredentialsRequiredException e)
            {
            /*
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, true);
            }
            populateContextMapForStaticTableByAnonymous(contextMap, columnNames, url, appLink, forceAnonymous, useCache);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
            */
            }
            catch (MalformedRequestException e)
            {
                //throwMacroExecutionException(e, conversionContext);
            }
            catch (Exception e)
            {
                //throwMacroExecutionException(e, conversionContext);
            }

        }
        catch (Exception e)
        {
            //throw new MacroExecutionException(e);
        }
        return null;
    }

    private String getXmlUrl(String requestData, JiraIssuesMacro.Type requestType,
                             ApplicationLink appLink) throws MacroExecutionException {
        StringBuffer sf = new StringBuffer(JiraUtil.normalizeUrl(appLink.getRpcUrl()));

        sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                .append(JiraUtil.MAXIMUM_ISSUES).append("&returnMax=true").append("&validateQuery=false").append("&jqlQuery=");
        sf.append(JiraUtil.utf8Encode(requestData));
        return sf.toString();
    }


    protected JiraRequestData parseRequestData(Map<String, String> params) {
        return new JiraRequestData(params.get(JQL_QUERY), JiraIssuesMacro.Type.JQL);
    }


    protected I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }

    String getText(String i18n)
    {
        return getI18NBean().getText(i18n);
    }

    String getText(String i18n, List substitutions)
    {
        return getI18NBean().getText(i18n, substitutions);
    }

    private boolean getBooleanProperty(Object value)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value).booleanValue();
        }
        else if (value instanceof String)
        {
            return BooleanUtils.toBoolean((String) value);
        }
        else
        {
            return false;
        }
    }
}
