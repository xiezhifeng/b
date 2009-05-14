package com.atlassian.confluence.extra.jira;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.SimpleStringCache;
import com.atlassian.confluence.extra.jira.cache.StringCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class JiraIssuesServlet extends HttpServlet
{
    private static final Logger log = Logger.getLogger(JiraIssuesServlet.class);

    private CacheManager cacheManager;

    private JiraIssuesManager jiraIssuesManager;

    private JiraIssuesResponseGenerator jiraIssuesResponseGenerator;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraIssuesResponseGenerator(JiraIssuesResponseGenerator jiraIssuesResponseGenerator)
    {
        this.jiraIssuesResponseGenerator = jiraIssuesResponseGenerator;
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
            boolean useTrustedConnection = BooleanUtils.toBoolean(request.getParameter("useTrustedConnection"));
            boolean useCache = BooleanUtils.toBoolean(request.getParameter("useCache"));

            String url = request.getParameter("url");
            String resultsPerPage = request.getParameter("rp");
            String page = request.getParameter("page");
            String sortField = request.getParameter("sortname");
            String sortOrder = request.getParameter("sortorder");

            // TODO: CONFJIRA-11: would be nice to check if url really points to a jira to prevent potentially being an open relay, but how exactly to do the check?
            /* URL suitable to be used as a cache key */
            String jiraIssueXmlUrlWithoutPaginationParam = jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, resultsPerPage, sortField, sortOrder);
            /* URL not suitable to be used as cache key, unless we want caches to be blown up with many duplicate values */
            String jiraIssueXmlUrlWithPaginationParam = jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, resultsPerPage, page, sortField, sortOrder);
            String retrieveJiraIssueXmlurl = StringUtils.isBlank(page) ? jiraIssueXmlUrlWithoutPaginationParam : jiraIssueXmlUrlWithPaginationParam;

            // generate issue data out in json format
            String jiraResponseAsJson = getResultJson(
                    new CacheKey(jiraIssueXmlUrlWithoutPaginationParam, columnsList, showCount, useTrustedConnection),
                    useTrustedConnection,
                    useCache,
                    parsePageParam(page),
                    showCount,
                    retrieveJiraIssueXmlurl);

            response.setContentType("application/json");

            out = response.getWriter();
            out.write(jiraResponseAsJson);
            out.flush();
        }
        catch (IOException e)
        {
            errorMessage = formatErrorMessage(e);
            log.warn("An IO Exception has been encountered: " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            errorMessage = StringUtils.isBlank(e.getMessage()) ? "Unable to parse parameters" : e.getMessage();
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
        
        return errorMessageBuilder.toString();
    }

    protected String getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage, boolean showCount, String url) throws Exception
    {
        SimpleStringCache subCacheForKey = getSubCacheForKey(key, !useCache);
        String jiraResponseAsJson = subCacheForKey.get(requestedPage);

        if (jiraResponseAsJson != null)
            return jiraResponseAsJson;

        // TODO: time this with macroStopWatch?
        // and log more debug statements?

        // get data from jira and transform into json
        log.debug("Retrieving issues from URL: " + url);
        JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXML(url, useTrustedConnection);

        jiraResponseAsJson = jiraIssuesResponseGenerator.generate(channel, key.getColumns(), requestedPage, showCount);
        subCacheForKey.put(requestedPage, jiraResponseAsJson);
        
        return jiraResponseAsJson;
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

        SimpleStringCache subCacheForKey = (SimpleStringCache)cacheCache.get(key);
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
