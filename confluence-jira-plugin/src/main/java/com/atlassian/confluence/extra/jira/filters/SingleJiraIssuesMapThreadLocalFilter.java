package com.atlassian.confluence.extra.jira.filters;

import com.atlassian.confluence.extra.jira.SingleJiraIssuesMapThreadLocal;

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
            SingleJiraIssuesMapThreadLocal.init();
            chain.doFilter(request, response);
        }
        finally
        {
            SingleJiraIssuesMapThreadLocal.dispose();
        }
    }

    @Override
    public void destroy() {

    }
}
