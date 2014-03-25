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
     * @param request  see {@link javax.servlet.ServletRequest}
     * @param response see {@link javax.servlet.ServletResponse}
     * @param chain    see {@link javax.servlet.FilterChain}
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            SingleJiraIssuesThreadLocalAccessor.init();
            chain.doFilter(request, response);
        }
        finally
        {
            SingleJiraIssuesThreadLocalAccessor.dispose();
            long elapsedTime = System.currentTimeMillis() - start;
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("******* Total execution time: {} milliseconds", elapsedTime);
            }
        }
    }

    @Override
    public void destroy()
    {
        // no-op
    }
}
