package com.atlassian.confluence.extra.jira.filters;

import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;
import com.atlassian.confluence.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

public class SingleJiraIssuesMapThreadLocalFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesMapThreadLocalFilter.class);
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
            log.debug("Total execution time before decoration: " + elapsedTime + " milliseconds");
        }
    }

    @Override
    public void destroy() {

    }
}
