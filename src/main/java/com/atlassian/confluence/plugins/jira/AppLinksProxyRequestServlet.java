package com.atlassian.confluence.plugins.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.ApplicationType;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.net.Request.MethodType;

public class AppLinksProxyRequestServlet extends AbstractProxyServlet
{
    
    private static final String APP_TYPE = "appType";
    private static final String APP_ID = "appId";
    private static final String JSON_STRING = "jsonString";
    private static final String FORMAT_ERRORS = "formatErrors";
    private static final String PATH = "path";
    private static Set<String> reservedParameters = new HashSet<String>(Arrays.asList(PATH, JSON_STRING, APP_ID, APP_TYPE, FORMAT_ERRORS));
    
    public AppLinksProxyRequestServlet(ApplicationLinkService appLinkService)
    {
        super(appLinkService);
    }
    
    @SuppressWarnings("unchecked")
    void doProxy(final HttpServletRequest req, final HttpServletResponse resp, final MethodType methodType) throws IOException, ServletException
    {
        String url = req.getParameter(PATH);
        if (url == null)
        {
            url = req.getHeader("X-AppPath");
        }
        final Map parameters = req.getParameterMap();
       
        String queryString = "";
        for (Object name : parameters.keySet())
        {
            if (reservedParameters.contains(name))
            {
                continue; 
            }
            
            Object val = parameters.get(name);
            if (val instanceof String[])
            {
                String[] params = (String[])val;
                for (String param : params)
                {
                    queryString = queryString + (queryString.length() > 0 ? "&" : "") + URLEncoder.encode(name.toString(), "UTF-8") + "=" + URLEncoder.encode(param, "UTF-8");;
                }                
            }
            else
            {
                queryString = queryString + (queryString.length() > 0 ? "&" : "") + URLEncoder.encode(name.toString(), "UTF-8") + "=" + URLEncoder.encode(req.getParameter(name.toString()), "UTF-8");;
            }
            
        }
        if (methodType == MethodType.GET && queryString .length() > 0)
        {
            url = url + (url.contains("?") ? '&' : '?') + queryString;
        }
        super.doProxy(resp, req, methodType, url);
    }
    
}
