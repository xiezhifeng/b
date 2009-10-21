package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.renderer.v2.macro.basic.validator.MacroParameterValidationException;
import com.opensymphony.webwork.ServletActionContext;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro
{
    private static final Logger LOG = Logger.getLogger(JiraIssuesMacro.class);

	private static final String RENDER_MODE_PARAM = "renderMode";
	private static final String STATIC_RENDER_MODE = "static";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList(
            "type", "key", "summary", "assignee", "reporter",
            "priority", "status", "resolution", "created", "updated", "due");
    private static final Set<String> WRAPPED_TEXT_FIELDS = new HashSet<String>(Arrays.asList("summary", "component", "version", "description"));


    private static final int PARAM_POSITION_1 = 1;
    private static final int PARAM_POSITION_2 = 2;
    private static final int PARAM_POSITION_4 = 4;
    private static final int PARAM_POSITION_5 = 5;
    private static final int PARAM_POSITION_6 = 6;
    
    private JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private I18NBeanFactory i18NBeanFactory;
    
    private JiraIssuesManager jiraIssuesManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    private I18NBean getI18NBean()
    {
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

    public boolean isInline()
    {
        return false;
    }

    public boolean hasBody()
    {
        return false;
    }

    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraIssuesColumnManager(JiraIssuesColumnManager jiraIssuesColumnManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }

    public void setTrustedApplicationConfig(TrustedApplicationConfig trustedApplicationConfig)
    {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }

    private boolean isTrustWarningsEnabled()
    {
        return null != trustedApplicationConfig && trustedApplicationConfig.isTrustWarningsEnabled();
    }

    private boolean isUseTrustTokens()
    {
        return null != trustedApplicationConfig && trustedApplicationConfig.isUseTrustTokens();
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        @SuppressWarnings("unchecked")
        Map<String, String> typeSafeParams = (Map<String, String>) params;

        boolean showCount = BooleanUtils.toBoolean(typeSafeParams.get("count"));
        boolean renderInHtml = shouldRenderInHtml(typeSafeParams.get(RENDER_MODE_PARAM), renderContext);
        createContextMapFromParams(typeSafeParams, contextMap, renderInHtml, showCount);

        if(renderInHtml && showCount) // TODO: match to current markup (span etc...)
            return "<span class=\"jiraissues_count\"><a href=\"" + GeneralUtil.htmlEncode((String)contextMap.get("clickableUrl")) + "\">" + contextMap.get("count") + " " + getText("jiraissues.issues.word") + "</a></span>";
        else if(renderInHtml)
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/staticJiraIssues.vm", contextMap);
        else if(showCount)
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/showCountJiraissues.vm", contextMap);
        else
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues.vm", contextMap);
    }

    protected void createContextMapFromParams(Map<String, String> params, Map<String, Object> contextMap, boolean renderInHtml, boolean showCount) throws MacroException
    {
        String url = getUrlParam(params);
        List<ColumnInfo> columns = getColumnInfo(getParam(params,"columns", PARAM_POSITION_1));
        contextMap.put("columns", columns);
        String cacheParameter = getParam(params,"cache", PARAM_POSITION_2);

        contextMap.put("title", "jiraissues.title");
        if (params.containsKey("title"))
            contextMap.put("title", GeneralUtil.htmlEncode(params.get("title")));

        // maybe this should change to position 3 now that the former 3 param got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = getParam(params,"anonymous", PARAM_POSITION_4);
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        // and maybe this should change to position 4 -- see comment for anonymousStr above
        String forceTrustWarningsStr = getParam(params,"forceTrustWarnings", PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
            forceTrustWarningsStr = "false";

        contextMap.put("width", StringUtils.defaultString(params.get("width"), DEFAULT_DATA_WIDTH));
        String heightStr = getParam(params, "height", PARAM_POSITION_6);
        if (StringUtils.isEmpty(heightStr) || !StringUtils.isNumeric(heightStr))
            heightStr = null;

        boolean useCache = StringUtils.isBlank(cacheParameter) || cacheParameter.equals("on") || Boolean.valueOf(cacheParameter);
        boolean useTrustedConnection = isUseTrustTokens() && !Boolean.valueOf(anonymousStr) && !SeraphUtils.isUserNamePasswordProvided(url);
        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr) || isTrustWarningsEnabled();
        contextMap.put("showTrustWarnings", showTrustWarnings);

        StringBuffer urlBuffer = new StringBuffer(url);

        if (renderInHtml)
        {
            try
            {
                JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXML(url, useTrustedConnection);
                Element element = channel.getChannelElement();

                if(showCount)
                {
                    Element totalItemsElement = element.getChild("issue");
                    String count = totalItemsElement!=null ? totalItemsElement.getAttributeValue("total") : ""+element.getChildren("item").size();
                    contextMap.put("count", count);
                }
                else
                {
                    contextMap.put("trustedConnection", channel.isTrustedConnection());
                    contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
                    contextMap.put("channel", element);
                    contextMap.put("entries", element.getChildren("item"));
                    contextMap.put("icons", jiraIssuesManager.getIconMap(element));
                    contextMap.put("xmlXformer", xmlXformer);
                    contextMap.put("jiraIssuesManager", jiraIssuesManager);
                }
            }
            catch (IOException e)
            {
                throw new MacroException("Unable to retrieve issue data", e);
            }
        }
        else
        {
            contextMap.put("resultsPerPage", getResultsPerPageParam(urlBuffer));

            // unfortunately this is ignored right now, because the javascript has not been made to handle this (which may require hacking and this should be a rare use-case)
            String startOn = getStartOnParam(params.get("startOn"), urlBuffer);
            contextMap.put("startOn",  new Integer(startOn));

            contextMap.put("sortOrder",  getSortOrderParam(urlBuffer));
            contextMap.put("sortField",  getSortFieldParam(urlBuffer));

            contextMap.put("useTrustedConnection", useTrustedConnection);
            contextMap.put("useCache", useCache);

            // name must end in "Html" to avoid auto-encoding
            contextMap.put("retrieverUrlHtml", buildRetrieverUrl(columns, urlBuffer.toString(), useTrustedConnection));
            if (null != heightStr)
                contextMap.put("height",  heightStr);

            try
            {
                contextMap.put("sortEnabled", shouldSortBeEnabled(urlBuffer.toString(), useTrustedConnection));
            }
            catch (UnknownHostException uhe)
            {
                LOG.error(uhe);
                throw new MacroException(getText("jiraissues.error.unknownhost", Arrays.asList(StringUtils.defaultString(uhe.getMessage()))), uhe);
            }
            catch (ConnectException ce)
            {
                LOG.error(ce);
                throw new MacroException(getText("jiraissues.error.unabletoconnect"));
            }
            catch (IOException e)
            {
                LOG.error(e);
                throw new MacroException(getText("jiraissues.error.unabletodeterminesort"), e);
            }
        }

        String clickableUrl = makeClickableUrl(url);
        String baseurl = params.get("baseurl");
        if (StringUtils.isNotEmpty(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());

        contextMap.put("clickableUrl",  clickableUrl);
    }


    protected String getParam(Map<String, String> params, String paramName, int paramPosition)
    {
        String param = params.get(paramName);
        if(param==null)
            param = StringUtils.defaultString(params.get(String.valueOf(paramPosition)));

        return param.trim();
    }

    // url needs its own method because in the v2 macros params with equals don't get saved into the map with numbered keys such as "0", unlike the old macros
    protected String getUrlParam(Map<String, String> params)
    {
        String url = params.get("url");
        if(url==null)
        {
            String allParams = params.get(Macro.RAW_PARAMS_KEY);
            int barIndex = allParams.indexOf('|');
            if(barIndex!=-1)
                url = allParams.substring(0,barIndex);
            else
                url = allParams;
        }

        /* Rewrite RSS XML urls */
        return cleanUrlParentheses(url).trim().replaceFirst("/sr/jira.issueviews:searchrequest.*-rss/", "/sr/jira.issueviews:searchrequest-xml/");
    }

    // for CONF-1672
    protected String cleanUrlParentheses(String url)
    {
        if (url.indexOf('(') > 0)
            url = url.replaceAll("\\(", "%28");

        if (url.indexOf(')') > 0)
            url = url.replaceAll("\\)", "%29");

        if (url.indexOf("&amp;") > 0)
            url = url.replaceAll("&amp;", "&");

        return url;
    }

    // if we have a filter url and an old version of jira, sorting should be off because jira didn't used to respect sorting params on filter urls
    private boolean shouldSortBeEnabled(String url, boolean useTrustedConnection) throws IOException
    {
        return jiraIssuesManager.isSortEnabled(url, useTrustedConnection);
    }

    private boolean shouldRenderInHtml(String renderModeParamValue, RenderContext renderContext) {
		return RenderContext.PDF.equals(renderContext.getOutputType())
            || RenderContext.WORD.equals(renderContext.getOutputType())
            || STATIC_RENDER_MODE.equals(renderModeParamValue)
            || RenderContext.EMAIL.equals(renderContext.getOutputType())
            || RenderContext.FEED.equals(renderContext.getOutputType())
            || RenderContext.HTML_EXPORT.equals(renderContext.getOutputType());
	}

    private String getSortFieldParam(StringBuffer urlBuffer)
    {
        String sortField = filterOutParam(urlBuffer,"sorter/field=");
        if (StringUtils.isNotEmpty(sortField))
            return sortField;
        else
            return null;
    }

    private String getSortOrderParam(StringBuffer urlBuffer)
    {
        String sortOrder = filterOutParam(urlBuffer,"sorter/order=");
        if (StringUtils.isNotEmpty(sortOrder))
            return sortOrder.toLowerCase();
        else
            return "desc";
    }


    private String getStartOnParam(String startOn, StringBuffer urlParam)
    {
        String pagerStart = filterOutParam(urlParam,"pager/start=");
        if(StringUtils.isNotEmpty(startOn))
            return startOn.trim();
        else
        {
            if (StringUtils.isNotEmpty(pagerStart))
                return pagerStart;
            else
                return "0";
        }
    }

    protected int getResultsPerPageParam(StringBuffer urlParam) throws MacroParameterValidationException
    {
        String tempMaxParam = filterOutParam(urlParam,"tempMax=");
        if (StringUtils.isNotEmpty(tempMaxParam))
        {
            int tempMax = Integer.parseInt(tempMaxParam);
            if (tempMax <= 0)
            {
            	throw new MacroParameterValidationException("The tempMax parameter in the JIRA url must be greater than zero.");
            }
            return tempMax;
        }
        else
        {
            return 500;
        }
    }

    protected static String filterOutParam(StringBuffer baseUrl, final String filter)
    {
        int tempMaxParamLocation = baseUrl.indexOf(filter);
        if(tempMaxParamLocation!=-1)
        {
            String value;
            int nextParam = baseUrl.indexOf("&",tempMaxParamLocation); // finding start of next param, if there is one. can't be ? because filter is before any next param
            if(nextParam!=-1)
            {
                value = baseUrl.substring(tempMaxParamLocation+filter.length(),nextParam);
                baseUrl.delete(tempMaxParamLocation,nextParam+1);
            }
            else
            {
                value = baseUrl.substring(tempMaxParamLocation+filter.length(),baseUrl.length());
                baseUrl.delete(tempMaxParamLocation-1,baseUrl.length()); // tempMaxParamLocation-1 to remove ?/& since it won't be used by next param in this case

            }
            return value;
        }
        else
            return null;
    }

    public String rebaseUrl(String clickableUrl, String baseUrl)
    {
        return clickableUrl.replaceFirst(
            "^" + // only at start of string
                ".*?" + // minimum number of characters (the schema) followed by...
                "://" + // literally: colon-slash-slash
                "[^/]+", // one or more non-slash characters (the hostname)
            baseUrl);
    }

    protected static String makeClickableUrl(String url)
    {
        StringBuffer link = new StringBuffer(url);
        filterOutParam(link, "view="); // was removing only view=rss but this way is okay as long as there's not another kind of view= that we should keep
        filterOutParam(link, "decorator="); // was removing only decorator=none but this way is okay as long as there's not another kind of decorator= that we should keep
        filterOutParam(link, "os_username=");
        filterOutParam(link, "os_password=");

        String linkString = link.toString();
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml\\?", "secure/IssueNavigator.jspa?reset=true&");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml", "secure/IssueNavigator.jspa?reset=true");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml\\?", "secure/IssueNavigator.jspa?requestId=$1&");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml", "secure/IssueNavigator.jspa?requestId=$1");
        return linkString;
    }


    protected List<ColumnInfo> getColumnInfo(String columnsParameter)
    {
        List<String> columnNames = DEFAULT_RSS_FIELDS;

        if (StringUtils.isNotBlank(columnsParameter))
        {
            columnNames = new ArrayList<String>();
            List<String> keys = Arrays.asList(columnsParameter.split(",|;"));
            for (String key : keys)
            {
                if(StringUtils.isNotBlank(key))
                {
                    columnNames.add(key);
                }
            }

            if( columnNames.isEmpty())
            {
                columnNames = DEFAULT_RSS_FIELDS;
            }
        }

        List<ColumnInfo> info = new ArrayList<ColumnInfo>();
        for (String columnName : columnNames)
        {
            String key = jiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName);

            String i18nKey = PROP_KEY_PREFIX + key;
            String displayName = getText(i18nKey);

            // getText() unexpectedly returns the i18nkey if a value isn't found
            if( StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
                displayName = columnName;

            info.add( new ColumnInfo(key, displayName));
        }

        return info;
    }


    private String buildRetrieverUrl(Collection<ColumnInfo> columns, String url, boolean useTrustedConnection)
    {
        HttpServletRequest req = ServletActionContext.getRequest();
        String baseUrl = req != null ? req.getContextPath() : "";
        StringBuffer retrieverUrl = new StringBuffer(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(utf8Encode(url));
        for (ColumnInfo columnInfo : columns)
        {
            retrieverUrl.append("&columns=").append(utf8Encode(columnInfo.toString()));
        }
        retrieverUrl.append("&useTrustedConnection=").append(useTrustedConnection);
        return retrieverUrl.toString();
    }

    private String utf8Encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // will never happen in a standard java runtime environment
            throw new RuntimeException("You appear to not be running on a standard Java Rutime Environment");
        }
    }

    public static class ColumnInfo
    {
        private static final String CLASS_NO_WRAP = "columns nowrap";
        private static final String CLASS_WRAP = "columns";


        private String title;
        private String rssKey;

        public ColumnInfo()
        {
        }

        public ColumnInfo(String rssKey)
        {
            this(rssKey, rssKey);
        }

        public ColumnInfo(String rssKey, String title)
        {
            this.rssKey = rssKey;
            this.title = title;
        }


        public String getTitle()
        {
            return title;
        }

        public String getKey()
        {
            return this.rssKey;
        }

        public String getHtmlClassName()
        {
            return shouldWrap() ? CLASS_WRAP : CLASS_NO_WRAP;
        }

        public boolean shouldWrap()
        {
            return WRAPPED_TEXT_FIELDS.contains(getKey().toLowerCase());
        }


        public String toString()
        {
            return getKey();
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof String)
            {
                String str = (String) obj;
                return this.rssKey.equalsIgnoreCase(str);
            }
            else if (obj instanceof ColumnInfo)
            {
                ColumnInfo that = (ColumnInfo) obj;
                return this.rssKey.equalsIgnoreCase(that.rssKey);
            }

            return false;
        }

        public int hashCode()
        {
            return this.rssKey.hashCode();
        }
    }

}
