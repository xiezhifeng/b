package com.atlassian.confluence.plugins.jira;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by tam on 3/17/14.
 */
public class SingleJiraIssuesThreadLocalFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        SingleJiraIssuesMapThreadLocal.dispose();
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
