package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.SimpleStringCache;
import com.atlassian.confluence.extra.jira.cache.StringCache;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.util.GeneralUtil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class JiraIssuesServlet extends HttpServlet
{
    private static final Logger log = Logger.getLogger(JiraIssuesServlet.class);

    private CacheManager cacheManager;

    private JiraIssuesManager jiraIssuesManager;

    private FlexigridResponseGenerator flexigridResponseGenerator;

    private JiraIssuesUrlManager jiraIssuesUrlManager;
    
    private ApplicationLinkService appLinkService;

    public void setApplicationLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }
    
    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraIssuesResponseGenerator(FlexigridResponseGenerator jiraIssuesResponseGenerator)
    {
        this.flexigridResponseGenerator = jiraIssuesResponseGenerator;
    }

    public void setJiraIssuesUrlManager(JiraIssuesUrlManager jiraIssuesUrlManager)
    {
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
    }

    private int parsePageParam(String pageString)
    {
        int page;
        try
        {
            page = StringUtils.isNotBlank(pageString) ? Integer.parseInt(pageString) : 0;
        }
        catch (NumberFormatException nfe)
        {
            log.debug("Unable to parse page parameter to an int: " + pageString);
            page = 0;
        }

        return page;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        Writer out = null;
        String errorMessage = null;
        
        try
        {
            List<String> columnsList = Arrays.asList(request.getParameterValues("columns"));
            boolean showCount = BooleanUtils.toBoolean(request.getParameter("showCount"));
            boolean forceAnonymous = BooleanUtils.toBoolean(request.getParameter("forceAnonymous"));
            boolean useCache = BooleanUtils.toBoolean(request.getParameter("useCache"));
            boolean flexigrid = BooleanUtils.toBoolean(request.getParameter("flexigrid"));

            String url = request.getParameter("url");
            String resultsPerPage = request.getParameter("rp");
            String page = request.getParameter("page");
            String sortField = request.getParameter("sortname");
            String sortOrder = request.getParameter("sortorder");
            String appIdStr = request.getParameter("appId");
            
            ApplicationLink applink = null;
            if (appIdStr != null)
            {
                applink = appLinkService.getApplicationLink(new ApplicationId(appIdStr));
            }
            
            // TODO: CONFJIRA-11: would be nice to check if url really points to a jira to prevent potentially being an open relay, but how exactly to do the check?
            /* URL suitable to be used as a cache key */
            String jiraIssueXmlUrlWithoutPaginationParam = jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, resultsPerPage, sortField, sortOrder);
            /* URL not suitable to be used as cache key, unless we want caches to be blown up with many duplicate values */
            String jiraIssueXmlUrlWithPaginationParam = jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, resultsPerPage, page, sortField, sortOrder);
            String retrieveJiraIssueXmlurl = StringUtils.isBlank(page) ? jiraIssueXmlUrlWithoutPaginationParam : jiraIssueXmlUrlWithPaginationParam;

            // generate issue data out in json format
            String jiraResponse = getResult(
                    new CacheKey(jiraIssueXmlUrlWithoutPaginationParam, appIdStr, columnsList, showCount, forceAnonymous, flexigrid),
                    applink,
                    forceAnonymous,
                    useCache,
                    parsePageParam(page),
                    showCount,
                    flexigrid,
                    retrieveJiraIssueXmlurl);
            
            if (flexigrid)
                response.setContentType("application/json");
            else
                response.setContentType("application/xml");

            out = response.getWriter();
            out.write(jiraResponse);
            out.flush();
        }
        catch (CredentialsRequiredException e)
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "OAuth realm=\"" + e.getAuthorisationURI().toString() + "\"");
        }
        catch (MalformedRequestException e)
        {
            response.setStatus(400);
        }
        catch (IOException e)
        {
            errorMessage = formatErrorMessage(e);
            log.warn("An IO Exception has been encountered: " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            errorMessage = StringUtils.isBlank(e.getMessage()) ? "Unable to parse parameters" : e.getMessage();
            errorMessage = GeneralUtil.htmlEncode(errorMessage);
            if (log.isDebugEnabled())
                log.debug("Unable to parse parameters" + e.getMessage(), e);
        }
        catch (Exception e)
        {
            errorMessage = formatErrorMessage(e);
            log.error("Unexpected Exception, could not retrieve JIRA issues: " + e.getMessage(), e);
        }
        finally
        {
            if (!StringUtils.isEmpty(errorMessage))
            {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try
                {
                    response.getWriter().write(errorMessage);
                }
                catch (IOException e)//ignore
                {}
            }
            IOUtils.closeQuietly(out);
        }
    }

    private String formatErrorMessage(Exception e) 
    {
        StringBuilder errorMessageBuilder = new StringBuilder();
        
        if (StringUtils.isNotBlank(e.getMessage()))
            errorMessageBuilder.append(e.getMessage()).append("<br/>");
        
        errorMessageBuilder.append(e.getClass().toString());
        
        return GeneralUtil.htmlEncode(errorMessageBuilder.toString());
    }

    protected String getResult(CacheKey key, ApplicationLink applink, boolean forceAnonymous, boolean useCache, int requestedPage, boolean showCount, boolean forFlexigrid, String url) throws Exception
    {
        SimpleStringCache subCacheForKey = getSubCacheForKey(key, !useCache);
        String jiraResponse = subCacheForKey.get(requestedPage);

        if (jiraResponse != null)
            return jiraResponse;

        // TODO: time this with macroStopWatch?
        // and log more debug statements?

        // get data from jira and transform into json
        log.debug("Retrieving issues from URL: " + url);
        if (forFlexigrid)
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, key.getColumns(), applink, forceAnonymous);
            jiraResponse = flexigridResponseGenerator.generate(channel, key.getColumns(), requestedPage, showCount, applink);
        }
        else
        {
            jiraResponse = jiraIssuesManager.retrieveXMLAsString(url, key.getColumns(), applink, forceAnonymous);
        }
        subCacheForKey.put(requestedPage, jiraResponse);
        
        return jiraResponse;
    }

    private SimpleStringCache getSubCacheForKey(CacheKey key, boolean flush)
    {
        /* Why am i using the JIRA Issues Macro's FQCN? There's one cache defined for it already. See confluence-coherence-cache-config.xml */
        Cache cacheCache = cacheManager.getCache(JiraIssuesMacro.class.getName());

        if (flush)
        {
            if (log.isDebugEnabled())
                log.debug("flushing cache for key: "+key);

            cacheCache.remove(key);
        }

        SimpleStringCache subCacheForKey = null;
        try
        {
            subCacheForKey = (SimpleStringCache)cacheCache.get(key);
        }
        catch (ClassCastException cce)
        {
            log.warn("Unable to get cached data with key " + key + ". The cached data will be purged", cce);
            cacheCache.remove(key);
        }

        if(subCacheForKey==null)
        {
            if(key.isShowCount())
                subCacheForKey = new StringCache(Collections.synchronizedMap(new HashMap()));
            else
                subCacheForKey = new CompressingStringCache(Collections.synchronizedMap(new HashMap()));
            cacheCache.put(key, subCacheForKey);
        }
        return subCacheForKey;
    }
}
