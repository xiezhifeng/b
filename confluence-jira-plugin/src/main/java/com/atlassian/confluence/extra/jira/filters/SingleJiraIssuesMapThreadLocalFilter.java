package com.atlassian.confluence.extra.jira.filters;

import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

/**
 * This filter is responsible for cleaning up the ThreadLocal maps managed by SingleJiraIssuesThreadLocalAccessor
 */
public class SingleJiraIssuesMapThreadLocalFilter implements Filter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesMapThreadLocalFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // no-op
    }

    /**
     * SingleJiraIssuesThreadLocalAccessor initializes all of its ThreadLocal maps before the request is dispatched
     * and disposes them before the response is returned to client
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        long start = System.currentTimeMillis();
        try
        {
            SingleJiraIssuesThreadLocalAccessor.init();
            chain.doFilter(request, response);
        }
        finally
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("******* Total execution time: {} milliseconds", System.currentTimeMillis() - start);
            }
        }
    }

    @Override
    public void destroy()
    {
        // no-op
    }
}
