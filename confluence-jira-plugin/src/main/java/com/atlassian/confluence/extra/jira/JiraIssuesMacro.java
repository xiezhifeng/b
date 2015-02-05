package com.atlassian.confluence.extra.jira;


import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.*;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRenderFactory;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;

import com.google.common.annotations.VisibleForTesting;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements Macro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesMacro.class);

    /**
     * Default constructor to get all necessary beans injected
     * @param i18NBeanFactory          see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param localeManager            see {@link com.atlassian.confluence.languages.LocaleManager}
     */
    public JiraIssuesMacro(I18NBeanFactory i18NBeanFactory, LocaleManager localeManager, JiraIssueRenderFactory jiraIssueRenderFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.localeManager = localeManager;
        this.jiraIssueRenderFactory = jiraIssueRenderFactory;
    }

    public static enum Type {KEY, JQL, URL}
    public static enum JiraIssuesType {SINGLE, COUNT, TABLE}

    // All context map's keys and parameters should be defined here to avoid unexpected typos and make the code clearer and easier for maintenance
    public static final String KEY = "key";
    public static final String JIRA = "jira";
    public static final String SHOW_SUMMARY = "showSummary";
    public static final String ITEM ="item";
    public static final String SERVER_ID = "serverId";
    public static final String CLICKABLE_URL = "clickableUrl";
    public static final String MOBILE = "mobile";
    public static final String SERVER = "server";
    public static final String TOKEN_TYPE_PARAM = ": = | TOKEN_TYPE | = :";
    public static final String RENDER_MODE_PARAM = "renderMode";
    public static final String POSITIVE_INTEGER_REGEX = "[0-9]+";
    public static final String JIRA_URL_KEY_PARAM = "url";
    public static final String JQL_QUERY = "jqlQuery";
    public static final String CACHE = "cache";
    public static final String COLUMNS = "columns";
    public static final String TITLE = "title";
    public static final String ANONYMOUS = "anonymous";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String DYNAMIC_RENDER_MODE = "dynamic";


    @VisibleForTesting
    static final String IS_NO_PERMISSION_TO_VIEW = "isNoPermissionToView";
    private static final String COUNT = "count";
    private static final String BASE_URL = "baseurl";
    private static final String MAXIMUM_ISSUES = "maximumIssues";
    private static final List<String> MACRO_PARAMS = Arrays.asList(
            COUNT, COLUMNS, TITLE, RENDER_MODE_PARAM, CACHE, WIDTH,
            HEIGHT, SERVER, SERVER_ID, ANONYMOUS, BASE_URL, SHOW_SUMMARY, com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY, MAXIMUM_ISSUES, TOKEN_TYPE_PARAM);

    private String resourcePath;

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private final I18NBeanFactory i18NBeanFactory;
    private final LocaleManager localeManager;
    private final JiraIssueRenderFactory jiraIssueRenderFactory;

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

    @Override
    public TokenType getTokenType(Map parameters, String body,
            RenderContext context)
    {
        String tokenTypeString = (String) parameters.get(TOKEN_TYPE_PARAM);
        if (org.apache.commons.lang.StringUtils.isBlank(tokenTypeString))
        {
            return TokenType.INLINE_BLOCK;
        }
        for (TokenType value : TokenType.values())
        {
            if (value.toString().equals(tokenTypeString))
            {
                return TokenType.valueOf(tokenTypeString);
            }
        }
        return TokenType.INLINE_BLOCK;
    }

    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext conversionContext)
    {
        try
        {
            JiraRequestData jiraRequestData = parseRequestData(parameters);
            JiraIssueRender jiraIssueRender = jiraIssueRenderFactory.getJiraIssueRender(jiraRequestData, parameters);
            return jiraIssueRender.getImagePlaceholder(jiraRequestData, parameters, resourcePath);
        }
        catch (MacroExecutionException e)
        {
            LOGGER.error("Error generate macro placeholder", e);
        }
        //return default placeholder
        return null;
    }

    public boolean hasBody()
    {
        return false;
    }

    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }


    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        try
        {
            return execute(params, body, new DefaultConversionContext(renderContext));
        }
        catch (MacroExecutionException e)
        {
            throw new MacroException(e);
        }
    }

    protected JiraRequestData parseRequestData(Map<String, String> params) throws MacroExecutionException {

        if(params.containsKey(JIRA_URL_KEY_PARAM))
        {
            return createJiraRequestData(params.get(JIRA_URL_KEY_PARAM), Type.URL);
        }

        if(params.containsKey(JQL_QUERY))
        {
            return createJiraRequestData(params.get(JQL_QUERY), Type.JQL);
        }

        if(params.containsKey(KEY))
        {
            return createJiraRequestData(params.get(KEY), Type.KEY);
        }

        String requestData = getPrimaryParam(params);
        if (requestData.startsWith("http"))
        {
            return createJiraRequestData(requestData, Type.URL);
        }

        Matcher keyMatcher = JiraJqlHelper.ISSUE_KEY_PATTERN.matcher(requestData);
        if (keyMatcher.find() && keyMatcher.start() == 0)
        {
            return createJiraRequestData(requestData, Type.KEY);
        }

        return createJiraRequestData(requestData, Type.JQL);
    }

    private JiraRequestData createJiraRequestData(String requestData, Type requestType) throws MacroExecutionException
    {
        if (requestType == Type.KEY && requestData.indexOf(',') != -1)
        {
            String jql = "issuekey in (" + requestData + ")";
            return new JiraRequestData(jql, Type.JQL);
        }

        if (requestType == Type.URL)
        {
            try
            {
                new URL(requestData);
                requestData = URIUtil.decode(requestData);
                requestData = URIUtil.encodeQuery(requestData);
            }
            catch(MalformedURLException e)
            {
                throw new MacroExecutionException(getText("jiraissues.error.invalidurl", Arrays.asList(requestData)), e);
            }
            catch (URIException e)
            {
                throw new MacroExecutionException(e);
            }

            requestData = cleanUrlParentheses(requestData).trim().replaceFirst("/sr/jira.issueviews:searchrequest.*-rss/", "/sr/jira.issueviews:searchrequest-xml/");
        }
        return new JiraRequestData(requestData, requestType);
    }

    // url needs its own method because in the v2 macros params with equals
    // don't get saved into the map with numbered keys such as "0", unlike the
    // old macros
    protected String getPrimaryParam(Map<String, String> params) throws MacroExecutionException {
        if(params.get("data") != null)
        {
            return params.get("data").trim();
        }

        Set<String> keys = params.keySet();
        for(String key : keys)
        {
            if(StringUtils.isNotBlank(key) && !MACRO_PARAMS.contains(key))
            {
                return key.matches(POSITIVE_INTEGER_REGEX) ? params.get(key) : key + "=" + params.get(key);
            }
        }

        throw new MacroExecutionException(getText("jiraissues.error.invalidMacroFormat"));
    }

    // for CONF-1672
    protected String cleanUrlParentheses(String url) {
        if (url.indexOf('(') > 0)
        {
            url = url.replaceAll("\\(", "%28");
        }

        if (url.indexOf(')') > 0)
        {
            url = url.replaceAll("\\)", "%29");
        }

        if (url.indexOf("&amp;") > 0)
        {
            url = url.replaceAll("&amp;", "&");
        }

        return url;
    }

    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException
    {
        JiraRequestData jiraRequestData = parseRequestData(parameters);
        jiraRequestData.setStaticMode(shouldRenderInHtml(parameters.get(RENDER_MODE_PARAM), conversionContext));

        JiraIssueRender jiraIssueRender = jiraIssueRenderFactory.getJiraIssueRender(jiraRequestData, parameters);
        return jiraIssueRender.renderMacro(jiraRequestData, parameters, conversionContext);
    }

    public String executeBatching(Map<String, String> parameters, ConversionContext conversionContext, Element issue, String serverUrl) throws Exception
    {
        return jiraIssueRenderFactory.getSingleJiraIssueRender().renderSingleJiraIssue(parameters, conversionContext, issue, serverUrl);
    }

    private boolean shouldRenderInHtml(String renderModeParamValue, ConversionContext conversionContext) {
        return RenderContext.PDF.equals(conversionContext.getOutputType())
                || RenderContext.WORD.equals(conversionContext.getOutputType())
                || !DYNAMIC_RENDER_MODE.equals(renderModeParamValue)
                || RenderContext.EMAIL.equals(conversionContext.getOutputType())
                || RenderContext.FEED.equals(conversionContext.getOutputType())
                || RenderContext.HTML_EXPORT.equals(conversionContext.getOutputType());
    }



    public BodyType getBodyType()
    {
        return BodyType.NONE;
    }

    public OutputType getOutputType()
    {
        return OutputType.BLOCK;
    }

    public String getResourcePath()
    {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

}
