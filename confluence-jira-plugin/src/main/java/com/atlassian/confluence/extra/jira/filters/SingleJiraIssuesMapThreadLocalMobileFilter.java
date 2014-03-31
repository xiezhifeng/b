package com.atlassian.confluence.extra.jira.filters;

import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * This filter is responsible for cleaning up the ThreadLocal maps managed by SingleJiraIssuesThreadLocalAccessor
 * when a page is rendered in mobile view mode
 */
public class SingleJiraIssuesMapThreadLocalMobileFilter implements Filter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesMapThreadLocalMobileFilter.class);

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
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
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
            SingleJiraIssuesThreadLocalAccessor.dispose();
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
