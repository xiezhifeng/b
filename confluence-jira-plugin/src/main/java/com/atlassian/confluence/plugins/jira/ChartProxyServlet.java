package com.atlassian.confluence.plugins.jira;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.extra.jira.model.PieChartModel;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;

public class ChartProxyServlet extends AbstractProxyServlet
{
    
    private static final Logger log = Logger.getLogger(ChartProxyServlet.class);
    
    private static final String PARAM_JQL = "jql";
    private static final String PARAM_STAT_TYPE = "statType";
    private static final String PARAM_CHART_TYPE = "chartType";
    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_HEIGHT = "height";
    

    public ChartProxyServlet(ApplicationLinkService appLinkService)
    {
        super(appLinkService);
    }
    
    @Override
    void doProxy(HttpServletRequest req, HttpServletResponse resp, MethodType methodType) throws IOException, ServletException
    {
        String jql = req.getParameter(PARAM_JQL);
        String chartType = req.getParameter(PARAM_CHART_TYPE);
        String appId = req.getParameter(PARAM_APP_ID);
        
        if (!StringUtils.hasLength(jql) || !StringUtils.hasLength(chartType) || !StringUtils.hasLength(appId)) 
        {
            resp.sendError(400, "Either jql, chartType or appId parameters is empty");
            return;
        }
        
        //TODO implement ChartEnum for registering all supported charts type and its behaviors.
        //Or better read it from gadget XML link ?
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-").append(URLEncoder.encode(jql,"UTF-8"));
        pathBuilder.append("&statType=").append(req.getParameter(PARAM_STAT_TYPE));
        if (req.getParameter(PARAM_WIDTH) != null)
        {
            pathBuilder.append("&width=").append(req.getParameter(PARAM_WIDTH));
        }
        if (req.getParameter(PARAM_HEIGHT) != null)
        {
            pathBuilder.append("&height=").append(req.getParameter(PARAM_HEIGHT));
        }
        
        super.doProxy(resp, req, methodType, pathBuilder.toString());
    }
    
    @Override
    protected void handleResponse(ApplicationLinkRequestFactory requestFactory, HttpServletRequest req, HttpServletResponse resp, ApplicationLinkRequest request, ApplicationLink appLink) throws ResponseException
    {
        String redirectLink = getRedirectImgLink(request, req, requestFactory, resp, appLink);
        if(redirectLink == null) {
            return;
        } 
        else
        {
            try
            {
                resp.sendRedirect(redirectLink);
            }
            catch (IOException e)
            {
                log.error("unable to send redirect to " + redirectLink, e);
            }
        }
    }
    
    protected String getRedirectImgLink(ApplicationLinkRequest request, HttpServletRequest req, ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp, ApplicationLink appLink) throws ResponseException
    {
        ChartProxyResponseHandler responseHandler = new ChartProxyResponseHandler(req, requestFactory, resp);
        Object ret = request.execute(responseHandler);
        if (ret == null) 
        {
            return null;
        }
        if (ret instanceof ByteArrayOutputStream) {
            ByteArrayInputStream in = new ByteArrayInputStream(((ByteArrayOutputStream) ret).toByteArray());
            //TODO implement chart type driven process here
            PieChartModel pieModel = null;
            try
            {
                pieModel = GsonHolder.gson.fromJson(new InputStreamReader(in), PieChartModel.class);
            }
            catch (Exception e)
            {
                log.error("Unable to parse jira chart macro json to object", e);
            }
            if (pieModel == null)
            {
                return null;
            }
            if (pieModel.getLocation() != null)
            {
                return appLink.getRpcUrl() + "/charts?filename=" + pieModel.getLocation();
            }
        }
        return null;
    }
    
    /**
     * Gson is thead-safe, so just use shared instance for all thread
     */
    private static final class GsonHolder
    {
        static final Gson gson = new Gson();
    }
    
    protected static class ChartProxyResponseHandler extends ProxyApplicationLinkResponseHandler
    {

        private ChartProxyResponseHandler(HttpServletRequest req,
                ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp)
        {
            super(req, requestFactory, resp);
        }
        
        @Override
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
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(responseStream, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    return outputStream;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            return null;
        }        
    }
}