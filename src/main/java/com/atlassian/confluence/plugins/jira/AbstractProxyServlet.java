package com.atlassian.confluence.plugins.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.ApplicationType;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.extra.jira.handlers.AbstractProxyResponseHandler;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;

public abstract class AbstractProxyServlet extends HttpServlet
{

    private static final String APP_TYPE = "appType";
    private static final String APP_ID = "appId";
    private static final String JSON_STRING = "jsonString";
    private static final String FORMAT_ERRORS = "formatErrors";
    private static final String PATH = "path";
    private static Set<String> reservedParameters = new HashSet<String>(Arrays.asList(PATH, JSON_STRING, APP_ID,
            APP_TYPE, FORMAT_ERRORS));
    protected static Set<String> headerWhitelist = new HashSet<String>(Arrays.asList("Content-Type", "Cache-Control",
            "Pragma"));

    protected ApplicationLinkService appLinkService;

    public AbstractProxyServlet(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    @Override
    protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
    {
        doProxy(req, resp, MethodType.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        doProxy(req, resp, MethodType.POST);
    }

    abstract void doProxy(final HttpServletRequest req, final HttpServletResponse resp, final MethodType methodType)
            throws IOException, ServletException;

    /**
     * 
     * @param resp
     * @param req
     * @param methodType
     * @param url
     *            the relative url to be retrieved
     * @throws IOException
     * @throws ServletException
     */
    protected void doProxy(final HttpServletResponse resp, final HttpServletRequest req, final MethodType methodType,
            String url) throws IOException, ServletException
    {
        String appId = req.getParameter(APP_ID);
        String appType = req.getParameter(APP_TYPE);
        if (appType == null && appId == null)
        {
            // look in the special headers
            appId = req.getHeader("X-AppId");
            appType = req.getHeader("X-AppType");
            if (appType == null && appId == null)
            {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "You must specify an appId or appType request parameter");
            }

        }
        ApplicationLink appLink = null;
        if (appId != null)
        {
            try
            {
                appLink = getApplicationLinkById(appId);
                if (appLink == null)
                {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No Application Link found for the id " + appId);
                }
            }
            catch (TypeNotInstalledException e)
            {
                throw new ServletException(e);
            }
        }
        else if (appType != null)
        {
            try
            {
                appLink = getPrimaryAppLinkByType(appType);
                if (appLink == null)
                {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No Application Link found for the type "
                            + appType);
                }
            }
            catch (ClassNotFoundException e)
            {
                throw new ServletException(e);
            }
        }

        // used for error reporting
        final String finalUrl = appLink.getRpcUrl() + url;
        final boolean formatErrors = Boolean.parseBoolean(req.getParameter(FORMAT_ERRORS));
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            ApplicationLinkRequest request = prepareRequest(req, methodType, url, requestFactory);
            request.setFollowRedirects(false);
            handleResponse(requestFactory, req, resp, request, appLink);
        }
        catch (ResponseException re)
        {
            handleProxyingException(formatErrors, finalUrl, resp, re);
        }
        catch (CredentialsRequiredException e)
        {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader("WWW-Authenticate", "OAuth realm=\"" + e.getAuthorisationURI().toString() + "\"");
        }
    }

    protected void handleResponse(ApplicationLinkRequestFactory requestFactory, HttpServletRequest req,
            HttpServletResponse resp, ApplicationLinkRequest request, ApplicationLink appLink) throws ResponseException
    {
        ProxyApplicationLinkResponseHandler responseHandler = new ProxyApplicationLinkResponseHandler(req,
                requestFactory, resp);
        request.execute(responseHandler);
    }

    protected final void handleProxyingException(boolean format, String finalUrl, HttpServletResponse resp, Exception e)
            throws IOException
    {
        String errorMsg = "There was an error proxying your request to " + finalUrl + " because of " + e.getMessage();
        resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, errorMsg);
    }

    protected static ApplicationLinkRequest prepareRequest(HttpServletRequest req, MethodType methodType, String url,
            final ApplicationLinkRequestFactory requestFactory) throws CredentialsRequiredException, IOException
    {
        ApplicationLinkRequest request = requestFactory.createRequest(methodType, url);
        request.setHeader("X-Atlassian-Token", "no-check");

        if (methodType == MethodType.POST)
        {

            String ctHeader = req.getHeader("Content-Type");
            if (ctHeader != null)
            {
                request.setHeader("Content-Type", ctHeader);
            }

            if (ctHeader != null && (ctHeader.contains("multipart/form-data") || ctHeader.contains("application/xml")))
            {
                String enc = req.getCharacterEncoding();
                String str = IOUtils.toString(req.getInputStream(), (enc == null ? "ISO8859_1" : enc));
                request.setRequestBody(str);
            }
            else
            {
                List<String> params = new ArrayList<String>();
                Map parameters = req.getParameterMap();
                for (Object name : parameters.keySet())
                {
                    if (reservedParameters.contains(name))
                    {
                        continue;
                    }
                    params.add(name.toString());
                    params.add(req.getParameter(name.toString()));
                }
                request.addRequestParameters((String[]) params.toArray(new String[0]));
            }
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    protected ApplicationLink getPrimaryAppLinkByType(String type) throws ClassNotFoundException
    {
        Class<? extends ApplicationType> clazz = (Class<? extends ApplicationType>) Class.forName(type);
        ApplicationLink primaryLink = appLinkService.getPrimaryApplicationLink(clazz);
        return primaryLink;
    }

    protected ApplicationLink getApplicationLinkById(String id) throws TypeNotInstalledException
    {
        ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(id));
        return appLink;
    }

    protected static class ProxyApplicationLinkResponseHandler extends AbstractProxyResponseHandler
    {

        protected ProxyApplicationLinkResponseHandler(HttpServletRequest req,
                ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp)
        {
            super(req, requestFactory, resp);
        }

        protected Object processSuccess(Response response) throws ResponseException
        {
            InputStream responseStream = response.getResponseBodyAsStream();
            Map<String, String> headers = response.getHeaders();
            for (String key : headers.keySet())
            {
                if (headerWhitelist.contains(key))
                {
                    resp.setHeader(key, headers.get(key));
                }
            }
            try
            {
                if (responseStream != null)
                {
                    ServletOutputStream outputStream = resp.getOutputStream();
                    IOUtils.copy(responseStream, outputStream);
                    outputStream.flush();
                    outputStream.close();
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            return null;
        }

        protected Object retryRequest(Response response) throws ResponseException
        {
            try
            {
                ApplicationLinkRequest request = prepareRequest(req, MethodType.GET, response.getHeader("location"),
                        requestFactory);
                request.setFollowRedirects(false);
                return request.execute(this);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e);
            }
            catch (CredentialsRequiredException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
