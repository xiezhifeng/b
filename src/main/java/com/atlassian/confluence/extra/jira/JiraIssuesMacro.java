package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.RenderContext;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.net.URLEncoder;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements TrustedApplicationConfig //, UserLocaleAware
{
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

    private final Logger log = Logger.getLogger(JiraIssuesMacro.class);
    private final Set defaultColumns = new LinkedHashSet();

    private final TrustedApplicationConfig trustedApplicationConfig = new JiraIssuesTrustedApplicationConfig();

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
        }
        return url;
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        try
        {
            Map contextMap = MacroUtils.defaultVelocityContext();
            createContextMapFromParams(params, renderContext, contextMap);
            return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues.vm", contextMap);
        }
        catch(Exception e)
        {
            return "Error. Message: "+e.getMessage();
        }
    }

    protected void createContextMapFromParams(Map params, RenderContext renderContext, Map contextMap) throws Exception
    {
        String url = getUrlParam(params);
        String columns = getParam(params,"columns", 1);
        String cacheParameter = getParam(params,"cache", 2);
        String template = getParam(params,"template", 3);
        boolean showCount = Boolean.valueOf(StringUtils.trim((String)params.get("count"))).booleanValue();
        String anonymousStr = getParam(params,"anonymous", 4); // TODO: also check in url for anon param there?
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        String forceTrustWarningsStr = getParam(params,"forceTrustWarnings", 5);
        if ("".equals(forceTrustWarningsStr))
            forceTrustWarningsStr = "false";

        boolean useCache = StringUtils.isBlank(cacheParameter) || cacheParameter.equals("on") || Boolean.valueOf(cacheParameter).booleanValue();
        boolean useTrustedConnection = trustedApplicationConfig.isUseTrustTokens() && !Boolean.valueOf(anonymousStr).booleanValue() && !SeraphUtils.isUserNamePasswordProvided(url);

        StringBuffer urlBuffer = new StringBuffer(url);

        contextMap.put("columns", prepareDisplayColumns(columns));
        contextMap.put("macroId", nextMacroId(renderContext));
        contextMap.put("showCount", new Boolean(showCount));

        String resultsPerPage = getResultsPerPageParam((String)params.get("resultsPerPage"), urlBuffer);
        contextMap.put("resultsPerPage", new Integer(resultsPerPage));

        // unfortunately this is ignored right now...
        String startOn = getStartOnParam((String)params.get("startOn"), urlBuffer);
        contextMap.put("startOn",  new Integer(startOn));

        contextMap.put("sortOrder",  getSortOrderParam(urlBuffer));
        contextMap.put("sortField",  getSortFieldParam(urlBuffer));

        contextMap.put("useTrustedConnection", new Boolean(useTrustedConnection));
        contextMap.put("useCache", new Boolean(useCache));

        contextMap.put("url", URLEncoder.encode(urlBuffer.toString(), "UTF-8"));

        String clickableUrl = makeClickableUrl(url);
        String baseurl = (String)params.get("baseurl");
        if (StringUtils.isNotEmpty(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());
        contextMap.put("clickableUrl",  clickableUrl);
    }

    // TODO: refactor all these methods to avoid duplication
    // *****************************************************

    private String getSortFieldParam(StringBuffer urlBuffer)
    {
        String sortField = filterOutParam(urlBuffer,"sorter/field=");
        if (StringUtils.isNotEmpty(sortField))
            return sortField;
        else
            return "updated";
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

    protected String getResultsPerPageParam(String resultsPerPageParam, StringBuffer urlParam)
    {
        String tempMax = filterOutParam(urlParam,"tempMax=");
        if(StringUtils.isNotEmpty(resultsPerPageParam))
            return resultsPerPageParam.trim();
        else
        {
            if (StringUtils.isNotEmpty(tempMax))
                return tempMax;
            else
                return ""+Integer.MAX_VALUE; //return "20"; // TODO: change the default back to 20 once don't need all results on one page
        }
    }

    // *****************************************************
    // END TODO

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
        return columnSet.isEmpty() ? defaultColumns : columnSet;
    }

    private String nextMacroId(RenderContext renderContext)
    {
        int macroId = 0;
        Integer id = (Integer) renderContext.getParam("nextGalleryId");
        if (id != null)
            macroId = id.intValue();
        renderContext.addParam("nextGalleryId", new Integer(macroId + 1));
        return "jiraissues_"+macroId;
    }
}



