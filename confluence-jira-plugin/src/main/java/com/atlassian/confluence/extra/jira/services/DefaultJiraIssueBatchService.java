package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

public class DefaultJiraIssueBatchService implements JiraIssueBatchService
{
    private static final Logger LOGGER = Logger.getLogger(JiraIssuesMacro.class);
    private static final String KEY = "key";
    private static final String ITEM = "item";
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

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     * @param parameters
     * @param keys
     * @param conversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.: http://jira.example.com/browse/
     * @throws MacroExecutionException
     */
    public Map<String, Object> getBatchResults(Map<String, String> parameters, Set<String> keys, ConversionContext conversionContext) throws MacroExecutionException {
        // make request to JIRA and build results
        Map<String, Object> map = Maps.newHashMap();
        Map<String, Element> elementMap = Maps.newHashMap();
        parameters.remove(KEY);

        StringBuilder jqlQueryBuilder = new StringBuilder().append("KEY IN (");

        for (String key : keys)
        {
            jqlQueryBuilder.append(key + ",");
        }
        jqlQueryBuilder.deleteCharAt(jqlQueryBuilder.length()-1).append(")");
        parameters.put(JQL_QUERY, jqlQueryBuilder.toString());

        JiraRequestData jiraRequestData = parseRequestData(parameters);

        JiraIssuesManager.Channel channel = retrieveChannel(parameters, jiraRequestData, conversionContext);
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
     * @param parameters
     * @param jiraRequestData
     * @param conversionContext
     * @return the Channel instance which represents the results we get from JIRA
     * @throws MacroExecutionException
     */
    private JiraIssuesManager.Channel retrieveChannel(Map<String, String> parameters, JiraRequestData jiraRequestData, ConversionContext conversionContext) throws MacroExecutionException
    {
        String requestData = jiraRequestData.getRequestData();
        JiraIssuesMacro.Type requestType = jiraRequestData.getRequestType();
        ApplicationLink appLink = null;
        try
        {
            appLink = applicationLinkResolver.resolve(requestType, requestData, parameters);
        }
        catch (TypeNotInstalledException tne)
        {
            throwMacroExecutionException(tne, conversionContext);
        }
        try
        {
            //parameters.putElement(TOKEN_TYPE_PARAM, TokenType.INLINE.name());
            List<String> columnNames = new ArrayList<String>();
            columnNames.add("type");
            columnNames.add("summary");
            columnNames.add("status");
            columnNames.add("resolution");
            columnNames.add("statusCategory");

            if (appLink != null)
            {
                String url = getXmlUrl(requestData, requestType, appLink);
                boolean clearCache = getBooleanProperty(conversionContext.getProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE));
                try
                {
                    if (clearCache)
                    {
                        jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, false, false);
                    }
                    // The 4rd parameter - forceAnonymous = false because we don't force anonymous
                    // The 5th parameter - useCache = false because we don't use cache here because single issue's status can be changed later
                    JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink,
                            false, false);
                    return channel;
                }
                catch (CredentialsRequiredException e)
                {
                    if (clearCache)
                    {
                        jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, false, true);
                    }
                }
                catch (MalformedRequestException e)
                {
                    throwMacroExecutionException(e, conversionContext);
                }
                catch (Exception e)
                {
                    throwMacroExecutionException(e, conversionContext);
                }
            }
            else
            {
                throw new MacroExecutionException(getText("jiraissues.error.noappLinks"));
            }
        }
        catch (MacroExecutionException e)
        {
            throw new MacroExecutionException(e);
        }
        return null;
    }

    private String getXmlUrl(String requestData, JiraIssuesMacro.Type requestType,
                             ApplicationLink appLink) throws MacroExecutionException
    {
        StringBuffer sf = new StringBuffer(JiraUtil.normalizeUrl(appLink.getRpcUrl()));

        sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                .append(JiraUtil.MAXIMUM_ISSUES).append("&returnMax=true").append("&validateQuery=false").append("&jqlQuery=");
        sf.append(JiraUtil.utf8Encode(requestData));
        return sf.toString();
    }


    protected JiraRequestData parseRequestData(Map<String, String> params)
    {
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

    /**
     * Wrap exception into MacroExecutionException. This exception then will be
     * processed by SingleJiraIssuesToViewTransformer.
     *
     * @param exception
     *            Any Exception thrown for whatever reason when Confluence could
     *            not retrieve JIRA Issues
     * @throws MacroExecutionException
     *             A macro exception means that a macro has failed to execute
     *             successfully
     */
    private void throwMacroExecutionException(Exception exception, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        String i18nKey = null;
        List params = null;

        if (exception instanceof UnknownHostException) {
            i18nKey = "jiraissues.error.unknownhost";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        } else if (exception instanceof ConnectException) {
            i18nKey = "jiraissues.error.unabletoconnect";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        } else if (exception instanceof AuthenticationException) {
            i18nKey = "jiraissues.error.authenticationerror";
        } else if (exception instanceof MalformedRequestException) {
            // JIRA returns 400 HTTP code when it should have been a 401
            i18nKey = "jiraissues.error.notpermitted";
        } else if (exception instanceof TrustedAppsException) {
            i18nKey = "jiraissues.error.trustedapps";
            params = Collections.singletonList(exception.getMessage());
        } else if (exception instanceof TypeNotInstalledException) {
            i18nKey = "jirachart.error.applicationLinkNotExist";
            params = Collections.singletonList(exception.getMessage());
        }

        if (i18nKey != null)
        {
            String msg = getText(getText(i18nKey, params));
            LOGGER.info(msg);
            LOGGER.debug("More info : ", exception);
            throw new MacroExecutionException(msg, exception);
        }
        else
        {
            if ( ! ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
            {
                LOGGER.error("Macro execution exception: ", exception);
            }
            throw new MacroExecutionException(exception);
        }
    }
}
