package com.atlassian.confluence.extra.jira;

import com.atlassian.core.util.FileUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestXML extends HttpServlet
{

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        try {
            response.getWriter().write(FileUtils.getResourceContent("/Users/ndang/Downloads/SearchRequest.xml"));
        } catch (Exception e) {
            System.out.print(e);
        }
    }

}
