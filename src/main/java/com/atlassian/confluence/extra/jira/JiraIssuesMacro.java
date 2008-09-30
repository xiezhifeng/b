package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.renderer.v2.macro.basic.validator.MacroParameterValidationException;
import com.opensymphony.webwork.ServletActionContext;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements TrustedApplicationConfig //, UserLocaleAware
{
	private static final String RENDER_MODE_PARAM = "renderMode";
	private static final String STATIC_RENDER_MODE = "static";
    private static final String DEFAULT_DATA_HEIGHT = "480";
    private static final int BUILD_NUM_FILTER_SORTING_WORKS = 328; // this isn't known to be the exact build number, but it is slightly greater than or equal to the actual number, and people shouldn't really be using the intervening versions anyway
    private static final Pattern filterUrlPattern = Pattern.compile("sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml");

    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList(new String[] { 
            "type", "key", "summary", "assignee", "reporter", 
            "priority", "status", "resolution", "created", "updated", "due" });
    private static final Set<String> WRAPPED_TEXT_FIELDS = new HashSet<String>(Arrays.asList(new String[] {"summary", "component", "version"}));

    
    private static final int PARAM_POSITION_1 = 1;
    private static final int PARAM_POSITION_2 = 2;
    private static final int PARAM_POSITION_3 = 3;
    private static final int PARAM_POSITION_4 = 4;
    private static final int PARAM_POSITION_5 = 5;
    private static final int PARAM_POSITION_6 = 6;

    private final TrustedApplicationConfig trustedApplicationConfig = new JiraIssuesTrustedApplicationConfig();
    private ConfluenceActionSupport confluenceActionSupport;
    private JiraIssuesUtils jiraIssuesUtils;
    
    private JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();
    
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

    public void setTrustWarningsEnabled(boolean enabled)
    {
        trustedApplicationConfig.setTrustWarningsEnabled(enabled);
    }

    protected ConfluenceActionSupport getConfluenceActionSupport()
    {
        if (null == confluenceActionSupport)
            confluenceActionSupport = GeneralUtil.newWiredConfluenceActionSupport();
        return confluenceActionSupport;
    }

    public void setUseTrustTokens(boolean enabled)
    {
       trustedApplicationConfig.setUseTrustTokens(enabled);
    }

    public boolean isTrustWarningsEnabled()
    {
        return trustedApplicationConfig.isTrustWarningsEnabled();
    }

    public boolean isUseTrustTokens()
    {
        return trustedApplicationConfig.isUseTrustTokens();
    }

    
    public String getName()
    {
        return "jiraissues";
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        boolean showCount = Boolean.valueOf(StringUtils.trim((String)params.get("count"))).booleanValue();
        boolean renderInHtml = shouldRenderInHtml(params, renderContext);
        createContextMapFromParams(params, contextMap, renderInHtml, showCount);

        if(renderInHtml && showCount) // TODO: match to current markup (span etc...)
            return "<span class=\"jiraissues_count\"><a href=\"" + GeneralUtil.htmlEncode((String)contextMap.get("clickableUrl")) + "\">" + contextMap.get("count") + " " + confluenceActionSupport.getText("jiraissues.issues.word") + "</a></span>";
        else if(renderInHtml)
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/staticJiraIssues.vm", contextMap);
        else if(showCount)
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/showCountJiraissues.vm", contextMap);
        else
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues.vm", contextMap);
    }

    protected void createContextMapFromParams(Map<String, Object> params, Map<String, Object> contextMap, boolean renderInHtml, boolean showCount) throws MacroException
    {
        String url = getUrlParam(params);
        List<ColumnInfo> columns = getColumnInfo(getParam(params,"columns", PARAM_POSITION_1));
        contextMap.put("columns", columns);
        String cacheParameter = getParam(params,"cache", PARAM_POSITION_2);

        // maybe this should change to position 3 now that the former 3 param got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = getParam(params,"anonymous", PARAM_POSITION_4);
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        // and maybe this should change to position 4 -- see comment for anonymousStr above
        String forceTrustWarningsStr = getParam(params,"forceTrustWarnings", PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
            forceTrustWarningsStr = "false";

        String heightStr = getParam(params, "height", PARAM_POSITION_6);
        if (StringUtils.isEmpty(heightStr) || !StringUtils.isNumeric(heightStr))
            heightStr = DEFAULT_DATA_HEIGHT;

        boolean useCache = StringUtils.isBlank(cacheParameter) || cacheParameter.equals("on") || Boolean.valueOf(cacheParameter).booleanValue();
        boolean useTrustedConnection = trustedApplicationConfig.isUseTrustTokens() && !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);
        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr).booleanValue() || isTrustWarningsEnabled();
        contextMap.put("showTrustWarnings", Boolean.valueOf(showTrustWarnings));

        StringBuffer urlBuffer = new StringBuffer(url);

        if (renderInHtml)
        {
            try
            {
                JiraIssuesUtils.Channel channel = jiraIssuesUtils.retrieveXML(url, useTrustedConnection);
                Element element = channel.getElement();

                if(showCount)
                {
                    Element totalItemsElement = element.getChild("issue");
                    String count = totalItemsElement!=null ? totalItemsElement.getAttributeValue("total") : ""+element.getChildren("item").size();
                    contextMap.put("count", count);
                }
                else
                {
                    contextMap.put("trustedConnection", Boolean.valueOf(channel.isTrustedConnection()));
                    contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
                    contextMap.put("channel", element);
                    contextMap.put("entries", element.getChildren("item"));
                    contextMap.put("icons", jiraIssuesUtils.prepareIconMap(element));
                    contextMap.put("xmlXformer", xmlXformer);
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
            String startOn = getStartOnParam((String)params.get("startOn"), urlBuffer);
            contextMap.put("startOn",  new Integer(startOn));

            contextMap.put("sortOrder",  getSortOrderParam(urlBuffer));
            contextMap.put("sortField",  getSortFieldParam(urlBuffer));

            contextMap.put("useTrustedConnection", Boolean.valueOf(useTrustedConnection));
            contextMap.put("useCache", Boolean.valueOf(useCache));

            // name must end in "Html" to avoid auto-encoding
            contextMap.put("retrieverUrlHtml", buildRetrieverUrl(columns, urlBuffer.toString(), useTrustedConnection));
            contextMap.put("height",  new Integer(heightStr));

            try
            {
                contextMap.put("sortEnabled", Boolean.valueOf(shouldSortBeEnabled(urlBuffer)));
            }
            catch (IOException e)
            {
                throw new MacroException("Unable to determine if sort should be enabled", e);
            }
        }

        String clickableUrl = makeClickableUrl(url);
        String baseurl = (String)params.get("baseurl");
        if (StringUtils.isNotEmpty(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());

        contextMap.put("clickableUrl",  clickableUrl);
    }

    
    protected String getParam(Map<String, Object> params, String paramName, int paramPosition)
    {
        String param = (String)params.get(paramName);
        if(param==null)
            param = StringUtils.defaultString((String)params.get(""+paramPosition));

        return param.trim();
    }

    // url needs its own method because in the v2 macros params with equals don't get saved into the map with numbered keys such as "0", unlike the old macros
    protected String getUrlParam(Map<String, Object> params)
    {
        String url = (String)params.get("url");
        if(url==null)
        {
            String allParams = (String)params.get(Macro.RAW_PARAMS_KEY);
            int barIndex = allParams.indexOf('|');
            if(barIndex!=-1)
                url = allParams.substring(0,barIndex);
            else
                url = allParams;
        }
        return url;
    }

    // if we have a filter url and an old version of jira, sorting should be off because jira didn't used to respect sorting params on filter urls
    private boolean shouldSortBeEnabled(StringBuffer urlBuffer) throws IOException
    {
        // if it is a filter url, try to check the jira version
        if(filterUrlPattern.matcher(urlBuffer.toString()).find())
        {
            String url = jiraIssuesUtils.getColumnMapKeyFromUrl(urlBuffer.toString());

            // look in cache first
            Boolean enableSortBoolean = jiraIssuesUtils.getSortSetting(url);
            if(enableSortBoolean!=null)
                return enableSortBoolean.booleanValue();

            // since not in cache or there but expired, make request to get xml header data and read the returned info
            boolean enableSort = makeJiraRequestToGetSorting(url);
            jiraIssuesUtils.putSortSetting(url, enableSort); // save result to bandana cache
            return enableSort;
        }
        else
            return true;
    }

    private boolean makeJiraRequestToGetSorting(String url) throws IOException
    {
        boolean enableSort = true;
        url += "?tempMax=0";
        JiraIssuesUtils.Channel channel = jiraIssuesUtils.retrieveXML(url, false);
        Element buildInfoElement = channel.getElement().getChild("build-info");
        if(buildInfoElement==null) // jira is older than when the version numbers went into the xml
            enableSort = false;
        else
        {
            Element buildNumberElement = buildInfoElement.getChild("build-number");
            String buildNumber = buildNumberElement.getValue();
            if(Integer.parseInt(buildNumber)<BUILD_NUM_FILTER_SORTING_WORKS) // if old version, no sorting
                enableSort = false;
        }
        return enableSort;
    }
    
    private boolean shouldRenderInHtml(Map params, RenderContext renderContext) {
		return RenderContext.PDF.equals(renderContext.getOutputType())
            || RenderContext.WORD.equals(renderContext.getOutputType())
            || STATIC_RENDER_MODE.equals(params.get(RENDER_MODE_PARAM))
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

    protected Integer getResultsPerPageParam(StringBuffer urlParam) throws MacroParameterValidationException
    {
        String tempMaxParam = filterOutParam(urlParam,"tempMax=");
        if (StringUtils.isNotEmpty(tempMaxParam))
        {
            Integer tempMax = new Integer(tempMaxParam);
            if (tempMax.intValue() <= 0)
            {
            	throw new MacroParameterValidationException("The tempMax parameter in the JIRA url must be greater than zero.");
            }
            return tempMax;
        }
        else
        {
            return new Integer(500);
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
        
        ConfluenceActionSupport actionSupport = getConfluenceActionSupport();
        
        List<ColumnInfo> info = new ArrayList<ColumnInfo>();
        for (String columnName : columnNames)
        {
            String key = xmlXformer.findBuiltinCanonicalForm(columnName);
            
            String i18nKey = PROP_KEY_PREFIX + key;
            String displayName = actionSupport.getText(i18nKey);
            
            // getText() unexpectedly returns the i18nkey if a value isn't found
            if( StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
                displayName = columnName;
            
            info.add( new ColumnInfo(key, displayName));
        }
        
        return info;
    }


    private String buildRetrieverUrl(Collection columns, String url, boolean useTrustedConnection)
    {
        HttpServletRequest req = ServletActionContext.getRequest();
        String baseUrl = req != null ? req.getContextPath() : "";
        StringBuffer retrieverUrl = new StringBuffer(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(utf8Encode(url));
        for (Iterator iterator = columns.iterator(); iterator.hasNext();)
        {
            retrieverUrl.append("&columns=").append(utf8Encode(iterator.next().toString()));
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
    
    
    public JiraIssuesUtils getJiraIssuesUtils()
    {
        return jiraIssuesUtils;
    }

    public void setJiraIssuesUtils(JiraIssuesUtils jiraIssuesUtils)
    {
        this.jiraIssuesUtils = jiraIssuesUtils;
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
