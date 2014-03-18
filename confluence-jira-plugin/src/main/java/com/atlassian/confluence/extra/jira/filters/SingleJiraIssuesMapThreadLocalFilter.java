package com.atlassian.confluence.extra.jira.filters;

import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;

import javax.servlet.*;
import java.io.IOException;

public class SingleJiraIssuesMapThreadLocalFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try
        {
            SingleJiraIssuesThreadLocalAccessor.init();
            chain.doFilter(request, response);
        }
        finally
        {
            SingleJiraIssuesThreadLocalAccessor.dispose();
        }
    }

    @Override
    public void destroy() {

    }
}
