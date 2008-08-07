package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.renderer.v2.macro.basic.validator.MacroParameterValidationException;
import com.opensymphony.util.TextUtils;
import com.opensymphony.webwork.ServletActionContext;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements TrustedApplicationConfig //, UserLocaleAware
{
	private static final String RENDER_MODE_PARAM = "renderMode";
	private static final String STATIC_RENDER_MODE = "static";
    private static final String DEFAULT_DATA_HEIGHT = "480";

    private final Set defaultColumns = new LinkedHashSet();

    private final TrustedApplicationConfig trustedApplicationConfig = new JiraIssuesTrustedApplicationConfig();
    private TrustedTokenAuthenticator trustedTokenAuthenticator;
    private JiraIconMappingManager jiraIconMappingManager;

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

    public void setTrustedTokenFactory(TrustedTokenFactory trustedTokenFactory)
    {
        this.trustedTokenAuthenticator = new TrustedTokenAuthenticator(trustedTokenFactory);
    }

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    public void setTrustWarningsEnabled(boolean enabled)
    {
        trustedApplicationConfig.setTrustWarningsEnabled(enabled);
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

    public void setDefaultColumns()
    {
        defaultColumns.clear();
        defaultColumns.add("type");
        defaultColumns.add("key");
        defaultColumns.add("summary");
        defaultColumns.add("assignee");
        defaultColumns.add("reporter");
        defaultColumns.add("priority");
        defaultColumns.add("status");
        defaultColumns.add("resolution");
        defaultColumns.add("created");
        defaultColumns.add("updated");
        defaultColumns.add("due");
    }

    public String getName()
    {
        return "jiraissues";
    }

    protected String getParam(Map params, String paramName, int paramPosition)
    {
        String param = (String)params.get(paramName);
        if(param==null)
            param = TextUtils.noNull((String)params.get(""+paramPosition));

        return param.trim();
    }

    // url needs its own method because in the v2 macros params with equals don't get saved into the map with numbered keys such as "0", unlike the old macros
    protected String getUrlParam(Map params)
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

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        Map contextMap = MacroUtils.defaultVelocityContext();
        createContextMapFromParams(params, renderContext, contextMap);
        return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues.vm", contextMap);
    }

    protected void createContextMapFromParams(Map params, RenderContext renderContext, Map contextMap) throws MacroException
    {
        String url = getUrlParam(params);
        Set columns = prepareDisplayColumns(getParam(params,"columns", 1));
        String cacheParameter = getParam(params,"cache", 2);
        boolean showCount = Boolean.valueOf(StringUtils.trim((String)params.get("count"))).booleanValue();
        boolean renderInHtml = !showCount && shouldRenderInHtml(params, renderContext);

        // maybe this should change to position 3 now that the former 3 param got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = getParam(params,"anonymous", 4);
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        // and maybe this should change to position 4 -- see comment for anonymousStr above
        String forceTrustWarningsStr = getParam(params,"forceTrustWarnings", 5);
        if ("".equals(forceTrustWarningsStr))
            forceTrustWarningsStr = "false";

        String heightStr = getParam(params, "height", 6);
        if (StringUtils.isEmpty(heightStr) || !StringUtils.isNumeric(heightStr))
            heightStr = DEFAULT_DATA_HEIGHT;

        boolean useCache = StringUtils.isBlank(cacheParameter) || cacheParameter.equals("on") || Boolean.valueOf(cacheParameter).booleanValue();
        boolean useTrustedConnection = trustedApplicationConfig.isUseTrustTokens() && !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);
        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr).booleanValue() || isTrustWarningsEnabled();
        contextMap.put("showTrustWarnings", Boolean.valueOf(showTrustWarnings));

        StringBuffer urlBuffer = new StringBuffer(url);

        contextMap.put("columns", columns);
        contextMap.put("showCount", Boolean.valueOf(showCount));
        contextMap.put("renderInHtml", Boolean.valueOf(renderInHtml));

        if (renderInHtml)
        {
            try
            {
                JiraIssuesUtils.Channel channel = JiraIssuesUtils.retrieveXML(url, useTrustedConnection, trustedTokenAuthenticator);
                Element element = channel.getElement();
                contextMap.put("channel", element);
                contextMap.put("entries", element.getChildren("item"));
                contextMap.put("icons", JiraIssuesUtils.prepareIconMap(element, jiraIconMappingManager));
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
        }

        String clickableUrl = makeClickableUrl(url);
        String baseurl = (String)params.get("baseurl");
        if (StringUtils.isNotEmpty(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());

        contextMap.put("clickableUrl",  clickableUrl);
    }

	private boolean shouldRenderInHtml(Map params, RenderContext renderContext) {
		return RenderContext.PDF.equals(renderContext.getOutputType())
            || RenderContext.WORD.equals(renderContext.getOutputType())
            || STATIC_RENDER_MODE.equals(params.get(RENDER_MODE_PARAM));
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

    private String makeClickableUrl(String url)
    {
        StringBuffer link = new StringBuffer(url);
        filterOutParam(link, "view="); // was removing only view=rss but this way is okay as long as there's not another kind of view= that we should keep
        filterOutParam(link, "decorator="); // was removing only decorator=none but this way is okay as long as there's not another kind of decorator= that we should keep
        filterOutParam(link, "os_username=");
        filterOutParam(link, "os_password=");
        return link.toString();
    }

    private Set prepareDisplayColumns(String columns)
    {
        setDefaultColumns();
        
        if (!TextUtils.stringSet(columns))
            return defaultColumns;

        Set columnSet = new LinkedHashSet(Arrays.asList(columns.split(",|;")));

        // if any item is empty, leave it out...
        Iterator columnSetIterator = columnSet.iterator();
        while(columnSetIterator.hasNext())
        {
            String columnName = (String)columnSetIterator.next();
            if(StringUtils.isEmpty(columnName))
                columnSetIterator.remove();
        }

        return columnSet.isEmpty() ? defaultColumns : columnSet;
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
}
